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

    public Optional<Execution> read(String jobId, ResourceResolver resourceResolver) throws ContentorException {
        Execution result = new ExecutionHistory(resourceResolver).read(jobId).orElse(null);
        if (result == null) {
            result = Optional.ofNullable(jobManager.getJobById(jobId)).map(QueuedExecution::new).orElse(null);
        }
        return Optional.ofNullable(result);
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

            if (immediateExecution.getStatus() == ExecutionStatus.SKIPPED) {
                LOG.info("Execution skipped '{}'", immediateExecution);
                return context.result().cancelled();
            } else {
                LOG.info("Execution succeeded '{}'", immediateExecution);
                return context.result().succeeded();
            }
        } catch (CancellationException e) {
            LOG.info("Execution aborted '{}'", queuedExecution);
            return context.result().cancelled();
        } catch (Exception e) {
            LOG.error("Execution failed '{}'", queuedExecution, e);
            return context.result().failed();
        }
    }

    private Execution executeAsync(QueuedExecution execution) throws ContentorException {
        try (ResourceResolver resolver = ResourceUtils.serviceResolver(resourceResolverFactory)) {
            ExecutionContext context = executor.createContext(execution.getExecutable(), resolver);
            context.setId(execution.getJob().getId());
            return executor.execute(context);
        } catch (LoginException e) {
            throw new ContentorException(String.format("Cannot access repository for execution '%s'", execution.getId()), e);
        }
    }
}
