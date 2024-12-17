package com.wttech.aem.contentor.core.code;

import com.wttech.aem.contentor.core.ContentorException;
import com.wttech.aem.contentor.core.util.ResourceUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
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

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionQueue.class);

    @ObjectClassDefinition(name = "AEM Contentor - Execution Queue")
    public @interface Config {

        @AttributeDefinition(name = "Async Poll Interval")
        long asyncPollInterval() default 500L;

        @AttributeDefinition(name = "Clean Poll Delay")
        long cleanPollDelay() default 3000L;
    }

    @Reference
    private JobManager jobManager;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Executor executor;

    private ExecutorService jobAsyncExecutor;

    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        this.jobAsyncExecutor = Executors.newSingleThreadExecutor();
    }

    @Deactivate
    protected void deactivate() {
        if (jobAsyncExecutor != null) {
            jobAsyncExecutor.shutdown();
        }
    }

    public ExecutionContext createContext(Executable executable, ResourceResolver resourceResolver) {
        return executor.createContext(executable, resourceResolver);
    }

    public Optional<Execution> submit(Executable executable) throws ContentorException {
        Job job = jobManager.addJob(TOPIC, Code.toJobProps(executable));
        if (job == null) {
            return Optional.empty();
        }
        return Optional.of(new QueuedExecution(job));
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

    @Override
    public JobExecutionResult process(Job job, JobExecutionContext context) {
        QueuedExecution queuedExecution = new QueuedExecution(job);

        LOG.info("Execution started '{}'", queuedExecution);

        Future<Execution> future = jobAsyncExecutor.submit(() -> {
            try {
                return executeAsync(queuedExecution);
            } catch (Throwable e) {
                throw new ContentorException(String.format("Execution failed asynchronously '%s'", queuedExecution), e);
            }
        });

        while (!future.isDone()) {
            if (context.isStopped() || Thread.currentThread().isInterrupted()) {
                future.cancel(true);
                LOG.info("Execution is cancelling '{}'", queuedExecution);
                break;
            }
            try {
                Thread.sleep(config.asyncPollInterval());
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
             OutputStream outputStream = Files.newOutputStream(ExecutionFile.path(execution.getJob().getId(), ExecutionFile.OUTPUT))) {
            ExecutionContext context = executor.createContext(execution.getExecutable(), resolver);
            context.setOutputStream(outputStream);
            return executor.execute(context);
        } catch (LoginException e) {
            throw new ContentorException(String.format("Cannot access repository for execution '%s'", execution.getId()), e);
        } catch (IOException e) {
            throw new ContentorException(String.format("Cannot write to files for execution '%s'", execution.getId()), e);
        }
    }

    private void cleanAsync(QueuedExecution execution) {
        try {
            Thread.sleep(config.cleanPollDelay());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.info("Execution clean up is interrupted '{}'", execution);
            return;
        }
        try {
            ExecutionFile.delete(execution.getJob().getId());
            LOG.info("Execution clean up succeeded '{}'", execution);
        } catch (ContentorException e) {
            LOG.error("Execution clean up failed '{}'", execution, e);
        }
    }
}
