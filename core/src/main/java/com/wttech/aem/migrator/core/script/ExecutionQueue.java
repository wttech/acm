package com.wttech.aem.migrator.core.script;

import com.wttech.aem.migrator.core.MigratorException;
import com.wttech.aem.migrator.core.instance.HealthChecker;
import com.wttech.aem.migrator.core.util.ResourceUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
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

@Component(
        immediate = true,
        service = {ExecutionQueue.class, JobExecutor.class},
        property = {JobExecutor.PROPERTY_TOPICS + "=" + ExecutionQueue.TOPIC})
public class ExecutionQueue implements JobExecutor {

    public static final String TOPIC = "com/wttech/aem/migrator/ExecutionQueue";

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

    public Optional<ExecutionJob> add(Executable executable) throws MigratorException {
        var job = jobManager.addJob(TOPIC, Code.toJobProps(executable));
        if (job == null) {
            return Optional.empty();
        }
        return Optional.of(new ExecutionJob(job.getId(), job.getJobState().name()));
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
        var executable = Code.fromJob(job);
        if (!healthChecker.isHealthy()) {
            LOG.warn("Failing execution '{}' - instance is not healthy.", executable);
            return context.result().failed();
        }

        LOG.info("Executing asynchronously '{}'", executable);
        var future = executorService.submit(() -> {
            try {
                executeAsync(executable, job);
            } catch (MigratorException e) {
                throw new RuntimeException(e); // TODO check if this bubbling up to parent thread
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
        }

        LOG.info("Executed asynchronously '{}'", executable);
        return context.result().succeeded();
    }

    private void executeAsync(Executable executable, Job job) throws MigratorException {
        try {
            Path outputFile = Files.createTempFile(StringUtils.replace(job.getId(), "/", "-"), ".log");
            LOG.info("Output file for job '{}' is '{}'", job.getId(), outputFile);
            try (var resolver = ResourceUtils.serviceResolver(resourceResolverFactory);
                    var output = Files.newOutputStream(outputFile)) {
                var options = new ExecutionOptions(resolver);
                options.setOutputStream(output);
                executor.execute(executable, options);
            } catch (LoginException e) {
                throw new MigratorException(
                        String.format(
                                "Failed to access repository while executing '%s' in job '%s'",
                                executable.getId(), job.getId()),
                        e);
            } catch (IOException e) {
                throw new MigratorException(
                        String.format("Cannot create output file for executable '%s' in job '%s'", job.getId()), e);
            }
        } catch (IOException e) {
            throw new MigratorException(
                    String.format(
                            "Cannot create output file for executable '%s' in job '%s'",
                            executable.getId(), job.getId()),
                    e);
        }
    }

    public Optional<ExecutionJob> find(String jobId) {
        var job = jobManager.getJobById(jobId);
        if (job == null) {
            return Optional.empty();
        }

        return Optional.of(new ExecutionJob(job.getId(), job.getJobState().name()));
    }

    public void stop(String jobId) {
        jobManager.stopJobById(jobId);
    }
}
