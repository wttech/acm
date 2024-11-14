package com.wttech.aem.migrator.core.script;

import com.wttech.aem.migrator.core.MigratorException;
import com.wttech.aem.migrator.core.instance.HealthChecker;
import com.wttech.aem.migrator.core.util.ResourceUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component(
        immediate = true,
        service = {ExecutionQueue.class, JobExecutor.class},
        property = {JobExecutor.PROPERTY_TOPICS + "=" + ExecutionQueue.TOPIC})
public class ExecutionQueue implements JobExecutor {

    public static final String TOPIC = "com/wttech/aem/migrator/ExecutionQueue";
    public static final String TMP_DIR = "migrator";
    private static final Logger LOG = LoggerFactory.getLogger(ExecutionQueue.class);
    @Reference
    private JobManager jobManager;

    @Reference
    private HealthChecker healthChecker;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Executor executor;

    private ExecutorService executorService;

    public Optional<Execution> submit(Executable executable) throws MigratorException {
        Job job = jobManager.addJob(TOPIC, Code.toJobProps(executable));
        if (job == null) {
            return Optional.empty();
        }

        return Optional.of(new Execution(executable, job.getId(), ExecutionStatus.of(job, null), 0, null, null));
    }

    public Optional<Execution> read(String jobId) throws MigratorException {
        Job job = jobManager.getJobById(jobId);
        if (job == null) {
            return Optional.empty();
        }

        Executable executable = Code.fromJob(job);
        String output = readFile(jobId, FileType.OUTPUT).orElse(null);
        String error = readFile(jobId, FileType.ERROR).orElse(null);
        long duration = calculateDuration(job).orElse(0L);
        ExecutionStatus status = ExecutionStatus.of(job, error);

        return Optional.of(new Execution(executable, job.getId(), status, duration, output, error));
    }

    public void stop(String jobId) {
        jobManager.stopJobById(jobId);
    }

    @Activate
    protected void activate() {
        executorService = Executors.newSingleThreadExecutor();
    }

    @Deactivate
    protected void deactivate() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Override
    public JobExecutionResult process(Job job, JobExecutionContext context) {
        Executable executable = Code.fromJob(job);
        if (!healthChecker.isHealthy()) {
            LOG.warn("Failing execution '{}' - instance is not healthy.", executable);
            return context.result().failed();
        }

        LOG.info("Executing asynchronously '{}'", executable);

        Future<?> future = executorService.submit(() -> {
            try {
                executeAsync(executable, job);
            } catch (Throwable e) {
                LOG.error("Executing asynchronously failed internally '{}'", executable, e);
            }
        });

        while (!future.isDone()) {
            if (context.isStopped() || Thread.currentThread().isInterrupted()) {
                future.cancel(true);
                LOG.info("Job '{}' is cancelling", executable);
                break;
            }
            try {
                Thread.sleep(1000); // TODO make this configurable
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.info("Job '{}' was interrupted", executable);
                return context.result().cancelled();
            }
        }

        try {
            future.get();
        } catch (CancellationException e) {
            LOG.info("Job '{}' is cancelled", executable);
            return context.result().cancelled();
        } catch (Exception e) {
            LOG.error("Error executing asynchronously '{}'", executable, e);
            return context.result().failed();
        } finally {
            // TODO clean up files after few seconds allowing to poll the result
        }

        LOG.info("Executed asynchronously '{}'", executable);
        return context.result().succeeded();
    }

    private void executeAsync(Executable executable, Job job) throws MigratorException {
        try (ResourceResolver resolver = ResourceUtils.serviceResolver(resourceResolverFactory);
             OutputStream outputStream = Files.newOutputStream(filePath(job.getId(), FileType.OUTPUT))) {
            ExecutionOptions options = new ExecutionOptions(resolver);
            options.setOutputStream(outputStream);

            Execution execution = executor.execute(executable, options);
            if (execution.getError() != null) {
                saveErrorToFile(executable, job, execution.getError());
            }
        } catch (LoginException e) {
            throw new MigratorException(
                    String.format(
                            "Cannot access repository while executing '%s' in job '%s'",
                            executable.getId(), job.getId()),
                    e);
        } catch (IOException e) {
            throw new MigratorException(
                    String.format(
                            "Cannot write to files for executable '%s' in job '%s'", executable.getId(), job.getId()),
                    e);
        }
    }

    private void saveErrorToFile(Executable executable, Job job, String error) {
        try (OutputStream errorStream = Files.newOutputStream(filePath(job.getId(), FileType.ERROR))) {
            IOUtils.write(error, errorStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Cannot write error file for executable '{}' in job '{}'", executable.getId(), job.getId(), e);
        }
    }

    private Optional<String> readFile(String jobId, FileType fileType) throws MigratorException {
        Path path = filePath(jobId, fileType);
        if (!path.toFile().exists()) {
            return Optional.empty();
        }

        try (InputStream input = Files.newInputStream(path)) {
            return Optional.ofNullable(IOUtils.toString(input, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new MigratorException(String.format("Cannot read output file for job '%s'", jobId), e);
        }
    }

    public Path filePath(String jobId, FileType kind) {
        File dir = FileUtils.getTempDirectory().toPath().resolve(TMP_DIR).toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.toPath()
                .resolve(String.format(
                        "%s_%s.log",
                        StringUtils.replace(jobId, "/", "-"), kind.name().toLowerCase()));
    }

    private Optional<Long> calculateDuration(Job job) {
        if (job == null || job.getFinishedDate() == null || job.getCreated() == null) {
            return Optional.empty();
        }
        return Optional.of(job.getFinishedDate().getTime().getTime()
                - job.getCreated().getTime().getTime());
    }

    public enum FileType {
        OUTPUT,
        ERROR
    }
}
