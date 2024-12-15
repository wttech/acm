package com.wttech.aem.contentor.core.code;

import com.wttech.aem.contentor.core.ContentorException;
import com.wttech.aem.contentor.core.util.ResourceUtils;
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

    public static final String TOPIC = "com/wttech/aem/contentor/ExecutionQueue";

    public static final String TMP_DIR = "contentor";

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionQueue.class);

    // TODO make this configurable
    private static final long EXECUTE_POLL_INTERVAL = 1000;

    // TODO make this configurable
    private static final long CLEAN_POLL_DELAY = 3000;

    @Reference
    private JobManager jobManager;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Executor executor;

    private ExecutorService jobAsyncExecutor;

    static Optional<String> readFile(String jobId, FileType fileType) throws ContentorException {
        Path path = filePath(jobId, fileType);
        if (!path.toFile().exists()) {
            return Optional.empty();
        }

        try (InputStream input = Files.newInputStream(path)) {
            return Optional.ofNullable(IOUtils.toString(input, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ContentorException(String.format("Cannot read output file for job '%s'", jobId), e);
        }
    }

    static Path filePath(String jobId, FileType kind) {
        File dir = FileUtils.getTempDirectory().toPath().resolve(TMP_DIR).toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.toPath()
                .resolve(String.format(
                        "%s_%s.log",
                        StringUtils.replace(jobId, "/", "-"), kind.name().toLowerCase()));
    }

    public ExecutionContext createContext(Executable executable, ResourceResolver resourceResolver) {
        return executor.createContext(executable, resourceResolver);
    }

    public Optional<Execution> submit(Executable executable) throws ContentorException {
        Job job = jobManager.addJob(TOPIC, Code.toJobProps(executable));
        if (job == null) {
            return Optional.empty();
        }
        return read(job.getId());
    }

    public Optional<Execution> read(String jobId) throws ContentorException {
        Job job = jobManager.getJobById(jobId);
        if (job == null) {
            return Optional.empty();
        }
        return Optional.of(new QueuedExecution(job));
    }

    public void stop(String jobId) {
        jobManager.stopJobById(jobId);
    }

    @Activate
    protected void activate() {
        jobAsyncExecutor = Executors.newSingleThreadExecutor();
    }

    @Deactivate
    protected void deactivate() {
        if (jobAsyncExecutor != null) {
            jobAsyncExecutor.shutdown();
        }
    }

    @Override
    public JobExecutionResult process(Job job, JobExecutionContext context) {
        QueuedExecution queuedExecution = new QueuedExecution(job);

        LOG.info("Execution started '{}'", queuedExecution);

        Future<Execution> future = jobAsyncExecutor.submit(() -> {
            try {
                return executeAsync(queuedExecution);
            } catch (Throwable e) {
                throw new ContentorException(String.format("Execution failed asynchronously internally '%s'", queuedExecution), e);
            }
        });

        while (!future.isDone()) {
            if (context.isStopped() || Thread.currentThread().isInterrupted()) {
                future.cancel(true);
                LOG.info("Execution is cancelling '{}'", queuedExecution);
                break;
            }
            try {
                Thread.sleep(EXECUTE_POLL_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.info("Execution is interrupted '{}'", queuedExecution);
                return context.result().cancelled();
            }
        }

        try {
            Execution immediateExecution = future.get();
            String message = QueuedExecution.jobResultMessage(immediateExecution);

            if (immediateExecution.getStatus() == ExecutionStatus.SKIPPED) {
                LOG.info("Execution skipped '{}'", immediateExecution);
                return context.result().message(message).cancelled();
            } else {
                LOG.info("Execution succeeded '{}'", immediateExecution);
                return context.result().message(message).succeeded();
            }
        } catch (CancellationException e) {
            LOG.info("Execution aborted '{}'", queuedExecution);
            return context.result().message(QueuedExecution.jobResultMessage(ExecutionStatus.ABORTED)).cancelled();
        } catch (Exception e) {
            LOG.error("Execution failed '{}'", queuedExecution, e);
            return context.result().failed();
        } finally {
            jobAsyncExecutor.submit(() -> {
                try {
                    cleanAsync(queuedExecution);
                } catch (Throwable e) {
                    LOG.error("Execution clean up failed '{}'", queuedExecution, e);
                }
            });
        }
    }

    private Execution executeAsync(QueuedExecution execution) throws ContentorException {
        try (ResourceResolver resolver = ResourceUtils.serviceResolver(resourceResolverFactory);
             OutputStream outputStream = Files.newOutputStream(filePath(execution.getJob().getId(), FileType.OUTPUT))) {
            ExecutionContext context = executor.createContext(execution.getExecutable(), resolver);
            context.setOutputStream(outputStream);
            return executor.execute(execution.getExecutable(), context);
        } catch (LoginException e) {
            throw new ContentorException(String.format("Cannot access repository for execution '%s'", execution.getId()), e);
        } catch (IOException e) {
            throw new ContentorException(String.format("Cannot write to files for execution '%s'", execution.getId()), e);
        }
    }

    private void cleanAsync(QueuedExecution execution) {
        try {
            Thread.sleep(CLEAN_POLL_DELAY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.info("Execution clean up is interrupted '{}'", execution);
            return;
        }

        try {
            Files.deleteIfExists(filePath(execution.getJob().getId(), FileType.OUTPUT));
            Files.deleteIfExists(filePath(execution.getJob().getId(), FileType.ERROR));
        } catch (IOException e) {
            LOG.error("Execution clean up failed '{}'", execution, e);
        }
    }

    public enum FileType {
        OUTPUT,
        ERROR
    }
}
