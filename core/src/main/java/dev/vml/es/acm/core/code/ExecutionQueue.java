package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.util.ResourceUtils;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
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

@Component(
        immediate = true,
        service = {ExecutionQueue.class, JobExecutor.class},
        property = {JobExecutor.PROPERTY_TOPICS + "=" + ExecutionQueue.TOPIC})
public class ExecutionQueue implements JobExecutor {

    public static final String TOPIC = "dev/vml/es/acm/ExecutionQueue";

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionQueue.class);

    @ObjectClassDefinition(name = "AEM Content Manager - Execution Queue")
    public @interface Config {

        @AttributeDefinition(name = "Async Poll Interval")
        long asyncPollInterval() default 500L;
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
        this.jobAsyncExecutor = Executors.newCachedThreadPool();
    }

    @Deactivate
    protected void deactivate() {
        if (jobAsyncExecutor != null) {
            jobAsyncExecutor.shutdown();
        }
    }

    public Execution submit(ExecutionContextOptions contextOptions, Executable executable)
            throws AcmException {
        Map<String, Object> jobProps = new HashMap<>();
        jobProps.putAll(ExecutionContextOptions.toJobProps(contextOptions));
        jobProps.putAll(Code.toJobProps(executable));
        jobProps.values().removeIf(Objects::isNull);

        Job job = jobManager.addJob(TOPIC, jobProps);
        if (job == null) {
            throw new AcmException(String.format("Execution of executable '%s' cannot be queued because manager refused to add a job!", executable.getId()));
        }
        return new QueuedExecution(executor, job);
    }

    public Stream<Execution> findAll() {
        return findJobs().map(job -> new QueuedExecution(executor, job));
    }

    public Stream<ExecutionSummary> findAllSummaries() {
        return findJobs().map(job -> new QueuedExecutionSummary(executor, job));
    }

    @SuppressWarnings("unchecked")
    private Stream<Job> findJobs() {
        return jobManager.findJobs(JobManager.QueryType.ALL, TOPIC, -1, Collections.emptyMap()).stream();
    }

    public Stream<Execution> readAll(Collection<String> executionIds) throws AcmException {
        return executionIds.stream()
                .filter(StringUtils::isNotBlank)
                .map(this::read)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    public Stream<ExecutionSummary> readAllSummaries(Collection<String> executionIds) throws AcmException {
        return executionIds.stream()
                .filter(StringUtils::isNotBlank)
                .map(this::readSummary)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    public Optional<Execution> read(String executionId) throws AcmException {
        return readJob(executionId).map(job -> new QueuedExecution(executor, job));
    }

    public Optional<ExecutionSummary> readSummary(String executionId) throws AcmException {
        return readJob(executionId).map(job -> new QueuedExecutionSummary(executor, job));
    }

    private Optional<Job> readJob(String executionId) {
        return Optional.ofNullable(jobManager.getJobById(executionId));
    }

    public void stop(String executionId) {
        jobManager.stopJobById(executionId);
    }

    @Override
    public JobExecutionResult process(Job job, JobExecutionContext context) {
        ExecutionContextOptions contextOptions = ExecutionContextOptions.fromJob(job);
        QueuedExecution queuedExecution = new QueuedExecution(executor, job);

        LOG.debug("Execution started '{}'", queuedExecution);

        Future<Execution> future = jobAsyncExecutor.submit(() -> {
            try {
                return executeAsync(contextOptions, queuedExecution);
            } catch (Throwable e) {
                throw new AcmException(String.format("Execution failed asynchronously '%s'", queuedExecution), e);
            }
        });

        while (!future.isDone()) {
            if (context.isStopped() || Thread.currentThread().isInterrupted()) {
                future.cancel(true);
                LOG.debug("Execution is cancelling '{}'", queuedExecution);
                break;
            }
            try {
                Thread.sleep(config.asyncPollInterval());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.debug("Execution is interrupted '{}'", queuedExecution);
                return context.result().cancelled();
            }
        }

        try {
            Execution immediateExecution = future.get();

            if (immediateExecution.getStatus() == ExecutionStatus.SKIPPED) {
                LOG.debug("Execution skipped '{}'", immediateExecution);
                return context.result()
                        .message(QueuedMessage.of(ExecutionStatus.SKIPPED).toJson())
                        .cancelled();
            } else {
                LOG.info("Execution succeeded '{}'", immediateExecution);
                return context.result().succeeded();
            }
        } catch (CancellationException e) {
            LOG.warn("Execution aborted '{}'", queuedExecution);
            return context.result()
                    .message(QueuedMessage.of(ExecutionStatus.ABORTED).toJson())
                    .cancelled();
        } catch (Exception e) {
            LOG.error("Execution failed '{}'", queuedExecution, e);
            return context.result().failed();
        }
    }

    private Execution executeAsync(ExecutionContextOptions contextOptions, QueuedExecution execution)
            throws AcmException {
        try (ResourceResolver resolver =
                        ResourceUtils.contentResolver(resourceResolverFactory, contextOptions.getUserId());
                ExecutionContext context = executor.createContext(
                        execution.getJob().getId(),
                        contextOptions.getExecutionMode(),
                        execution.getExecutable(),
                        resolver)) {
            return executor.execute(context);
        } catch (LoginException e) {
            throw new AcmException(String.format("Cannot access repository for execution '%s'", execution.getId()), e);
        }
    }
}
