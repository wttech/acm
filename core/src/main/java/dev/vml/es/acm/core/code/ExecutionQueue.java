package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.event.Event;
import dev.vml.es.acm.core.event.EventListener;
import dev.vml.es.acm.core.event.EventType;
import dev.vml.es.acm.core.gui.SpaSettings;
import dev.vml.es.acm.core.repo.Repo;
import dev.vml.es.acm.core.util.ExceptionUtils;
import dev.vml.es.acm.core.util.ResolverUtils;
import dev.vml.es.acm.core.util.StreamUtils;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.TopicStatistics;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        immediate = true,
        service = {ExecutionQueue.class, JobExecutor.class, EventListener.class},
        property = {JobExecutor.PROPERTY_TOPICS + "=" + ExecutionQueue.TOPIC})
@Designate(ocd = ExecutionQueue.Config.class)
@SuppressWarnings("java:S1181")
public class ExecutionQueue implements JobExecutor, EventListener {

    public static final String TOPIC = "dev/vml/es/acm/ExecutionQueue";

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionQueue.class);

    @ObjectClassDefinition(name = "AEM Content Manager - Execution Queue")
    public @interface Config {

        @AttributeDefinition(name = "Max Size", description = "Prevents overloading the system.")
        long maxSize() default 10;

        @AttributeDefinition(
                name = "Async Poll Interval",
                description = "Interval in milliseconds to poll for job status.")
        long asyncPollInterval() default 750L;

        @AttributeDefinition(
                name = "Abort Timeout",
                description = "Time in milliseconds to wait for graceful abort before forcing it.")
        long abortTimeout() default -1;
    }

    @Reference
    private JobManager jobManager;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private SpaSettings spaSettings;

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

    @Override
    public void onEvent(Event event) {
        EventType eventType = EventType.of(event.getName()).orElse(null);
        if (eventType == EventType.EXECUTOR_RESET) {
            reset();
        }
    }

    public Execution submit(Executable executable, ExecutionContextOptions contextOptions) throws AcmException {
        long currentSize = getCurrentSize();
        if (currentSize >= getMaxSize()) {
            throw new AcmException(String.format(
                    "Execution queue is full (%d/%d), cannot submit executable '%s'!",
                    currentSize, getMaxSize(), executable.getId()));
        }

        Map<String, Object> jobProps = new HashMap<>();
        jobProps.putAll(ExecutionContextOptions.toJobProps(contextOptions));
        jobProps.putAll(Code.toJobProps(executable));
        jobProps.values().removeIf(Objects::isNull);

        Job job = jobManager.addJob(TOPIC, jobProps);
        if (job == null) {
            throw new AcmException(String.format(
                    "Execution of executable '%s' cannot be queued because manager refused to add a job!",
                    executable.getId()));
        }
        return new QueuedExecution(executor, job, determineCodeOutput(job.getId()));
    }

    public Stream<Execution> findAll() {
        return findJobs().map(job -> new QueuedExecution(executor, job, determineCodeOutput(job.getId())));
    }

    public Optional<Execution> findByExecutableId(String executableId) {
        if (StringUtils.isBlank(executableId)) {
            return Optional.empty();
        }
        return findAll()
                .filter(e -> StringUtils.equals(e.getExecutable().getId(), executableId))
                .findFirst();
    }

    public boolean isStoppingOrStopped(String executionId) {
        Job job = readJob(executionId).orElse(null);
        if (job == null) {
            return false;
        }
        ExecutionStatus status = ExecutionStatus.of(job.getProperty(ExecutionJob.ACTIVE_STATUS_PROP, String.class))
                .orElse(null);
        if (ExecutionStatus.STOPPING.equals(status)) {
            return true;
        }
        return Job.JobState.STOPPED.equals(job.getJobState());
    }

    public Stream<ExecutionSummary> findAllSummaries() {
        return findJobs().map(job -> new QueuedExecutionSummary(executor, job));
    }

    @SuppressWarnings("unchecked")
    private Stream<Job> findJobs() {
        return jobManager.findJobs(JobManager.QueryType.ALL, TOPIC, -1, Collections.emptyMap()).stream();
    }

    // TODO use statistics to speed it up
    public long getCurrentSize() {
        return findJobs().count();
    }

    public TopicStatistics getStatistics() {
        return StreamUtils.asStream(jobManager.getTopicStatistics().iterator())
                .filter(ts -> TOPIC.equals(ts.getTopic()))
                .findFirst()
                .orElseThrow(() -> new AcmException(String.format("Cannot find statistics for topic '%s'!", TOPIC)));
    }

    public long getMaxSize() {
        return config.maxSize();
    }

    public boolean isFull() {
        return getCurrentSize() >= getMaxSize();
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
        return readJob(executionId).map(job -> new QueuedExecution(executor, job, determineCodeOutput(executionId)));
    }

    public Optional<ExecutionSummary> readSummary(String executionId) throws AcmException {
        return readJob(executionId).map(job -> new QueuedExecutionSummary(executor, job));
    }

    private Optional<Job> readJob(String executionId) {
        return Optional.ofNullable(jobManager.getJobById(executionId));
    }

    public void stop(String executionId) {
        Job job = readJob(executionId).orElse(null);
        if (job == null) {
            return;
        }
        setJobActiveStatus(job, ExecutionStatus.STOPPING);
        jobManager.stopJobById(job.getId());
    }

    private void setJobActiveStatus(Job job, ExecutionStatus status) {
        try {
            String path = FieldUtils.readField(job, "path", true).toString();
            ResolverUtils.useContentResolver(resourceResolverFactory, null, resolver -> {
                Repo.quiet(resolver).get(path).save(ExecutionJob.ACTIVE_STATUS_PROP, status.name());
            });
        } catch (Exception e) {
            throw new AcmException(
                    String.format("Cannot set execution '%s' job active status to '%s'!", job.getId(), status.name()),
                    e);
        }
    }

    private CodeOutput determineCodeOutput(String executionId) {
        return new CodeOutputRepo(resourceResolverFactory, spaSettings, executionId);
    }

    @Override
    public JobExecutionResult process(Job job, JobExecutionContext context) {
        ExecutionContextOptions contextOptions = ExecutionContextOptions.fromJob(job);
        QueuedExecution queuedExecution = new QueuedExecution(executor, job, new CodeOutputMemory());

        LOG.debug("Execution started '{}'", queuedExecution);

        Future<Execution> future = jobAsyncExecutor.submit(() -> {
            try {
                return executeAsync(contextOptions, queuedExecution);
            } catch (Throwable e) {
                throw new AcmException(String.format("Execution failed asynchronously '%s'", queuedExecution), e);
            }
        });

        Long abortStartTime = null;
        while (!future.isDone()) {
            if (context.isStopped() || isStoppingOrStopped(job.getId())) {
                if (abortStartTime == null) {
                    abortStartTime = System.currentTimeMillis();
                    if (config.abortTimeout() < 0) {
                        LOG.debug("Execution is aborting gracefully '{}' (no timeout)", queuedExecution);
                    } else {
                        LOG.debug("Execution is aborting '{}' (timeout: {}ms)", queuedExecution, config.abortTimeout());
                    }
                } else if (config.abortTimeout() >= 0) {
                    long abortDuration = System.currentTimeMillis() - abortStartTime;
                    if (abortDuration >= config.abortTimeout()) {
                        LOG.debug(
                                "Execution abort timeout exceeded ({}ms), forcing abort '{}'",
                                abortDuration,
                                queuedExecution);
                        future.cancel(true);
                        break;
                    }
                }
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
                        .message(QueuedMessage.of(ExecutionStatus.SKIPPED, null).toJson())
                        .cancelled();
            } else {
                LOG.debug("Execution succeeded '{}'", immediateExecution);
                return context.result().succeeded();
            }
        } catch (CancellationException e) {
            LOG.debug("Execution aborted forcefully '{}'", queuedExecution);
            return context.result()
                    .message(QueuedMessage.of(ExecutionStatus.ABORTED, ExceptionUtils.toString(e))
                            .toJson())
                    .cancelled();
        } catch (Exception e) {
            AbortException abortException = findAbortException(e);
            if (abortException != null) {
                LOG.debug("Execution aborted gracefully '{}'", queuedExecution);
                return context.result()
                        .message(QueuedMessage.of(ExecutionStatus.ABORTED, ExceptionUtils.toString(abortException))
                                .toJson())
                        .cancelled();
            }

            LOG.debug("Execution failed '{}'", queuedExecution, e);
            return context.result()
                    .message(QueuedMessage.of(ExecutionStatus.FAILED, ExceptionUtils.toString(e))
                            .toJson())
                    .failed();
        }
    }

    private AbortException findAbortException(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof AbortException) {
                return (AbortException) current;
            }
            current = current.getCause();
        }
        return null;
    }

    private Execution executeAsync(ExecutionContextOptions contextOptions, QueuedExecution execution)
            throws AcmException {
        try (ResourceResolver resolver =
                        ResolverUtils.contentResolver(resourceResolverFactory, contextOptions.getUserId());
                ExecutionContext context = executor.createContext(
                        execution.getJob().getId(),
                        contextOptions.getUserId(),
                        contextOptions.getExecutionMode(),
                        execution.getExecutable(),
                        contextOptions.getInputs(),
                        resolver,
                        determineOutput(
                                contextOptions.getExecutionMode(),
                                execution.getJob().getId()))) {
            context.setStatusUpdater(status -> setJobActiveStatus(execution.getJob(), status));
            return executor.execute(context);
        } catch (LoginException e) {
            throw new AcmException(String.format("Cannot access repository for execution '%s'", execution.getId()), e);
        }
    }

    private CodeOutput determineOutput(ExecutionMode mode, String executionId) {
        return mode == ExecutionMode.RUN
                ? new CodeOutputRepo(resourceResolverFactory, spaSettings, executionId)
                : new CodeOutputMemory();
    }

    public void reset() {
        if ((jobAsyncExecutor != null) && !jobAsyncExecutor.isShutdown()) {
            jobAsyncExecutor.shutdownNow();
        }
        jobAsyncExecutor = Executors.newCachedThreadPool();
        findJobs().forEach(job -> jobManager.removeJobById(job.getId()));

        // TODO maybe someday something like below - does not work due to outdated queues etc.
        // jobManager.getQueue(NAME).removeAll();
    }
}
