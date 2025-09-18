package dev.vml.es.acm.core.script;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.code.*;
import dev.vml.es.acm.core.code.schedule.BootSchedule;
import dev.vml.es.acm.core.code.schedule.CronSchedule;
import dev.vml.es.acm.core.code.schedule.NoneSchedule;
import dev.vml.es.acm.core.event.Event;
import dev.vml.es.acm.core.event.EventListener;
import dev.vml.es.acm.core.event.EventType;
import dev.vml.es.acm.core.instance.HealthChecker;
import dev.vml.es.acm.core.instance.HealthStatus;
import dev.vml.es.acm.core.osgi.InstanceInfo;
import dev.vml.es.acm.core.osgi.InstanceType;
import dev.vml.es.acm.core.repo.Repo;
import dev.vml.es.acm.core.util.ChecksumUtils;
import dev.vml.es.acm.core.util.ResolverUtils;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobBuilder;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = {ResourceChangeListener.class, EventListener.class, JobConsumer.class},
        immediate = true,
        property = {
            ResourceChangeListener.PATHS + "=glob:" + ScriptRepository.ROOT + "/automatic/**/*.groovy",
            ResourceChangeListener.CHANGES + "=ADDED",
            ResourceChangeListener.CHANGES + "=CHANGED",
            ResourceChangeListener.CHANGES + "=REMOVED",
            JobConsumer.PROPERTY_TOPICS + "=" + AcmConstants.CODE + "/script"
        })
@Designate(ocd = ScriptScheduler.Config.class)
public class ScriptScheduler implements ResourceChangeListener, EventListener, JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(ScriptScheduler.class);

    public static final String JOB_TOPIC = "dev/vml/es/acm/ScriptScheduler";

    public enum JobType {
        BOOT,
        CRON;

        public static JobType of(String value) {
            try {
                return JobType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Script scheduler job type is unsupported: " + value, e);
            }
        }
    }

    @ObjectClassDefinition(
            name = "AEM Content Manager - Script Scheduler",
            description = "Schedules automatic scripts on instance up and script changes")
    public @interface Config {

        @AttributeDefinition(
                name = "User Impersonation ID",
                description =
                        "Controls who accesses the repository when scripts are automatically executed. If blank, the service user 'acm-content-service' is used.")
        String userImpersonationId();

        @AttributeDefinition(
                name = "Health Check Retry Interval",
                description = "Interval in milliseconds to retry health check if instance is not healthy")
        long healthCheckRetryInterval() default 1000 * 10; // 10 seconds

        @AttributeDefinition(
                name = "Health Check Retry Count On Boot",
                description = "Maximum number of retries when checking instance health on boot script execution")
        long healthCheckRetryCountBoot() default 90; // * 10 seconds = 15 minutes

        @AttributeDefinition(
                name = "Health Check Retry Count On Cron Schedule",
                description =
                        "Maximum number of retries when checking instance health on cron schedule script execution")
        long healthCheckRetryCountCron() default 3; // * 10 seconds = 30 seconds
    }

    private Boolean instanceReady;

    private final Map<String, String> booted = new ConcurrentHashMap<>();

    private final Map<String, ScheduledJobInfo> scheduled = new ConcurrentHashMap<>();

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private InstanceInfo instanceInfo;

    @Reference
    private Executor executor;

    @Reference
    private JobManager jobManager;

    @Reference
    private HealthChecker healthChecker;

    @Reference
    private ExecutionQueue executionQueue;

    private Config config;

    @Activate
    protected void activate(Config config) {
        this.config = config;

        if (checkInstanceReady()) {
            bootWhenInstanceUp();
        }
    }

    @Modified
    protected void modify(Config config) {
        this.config = config;
    }

    @Deactivate
    protected void deactivate() {
        unscheduleBoot();
        unscheduleScripts();
        booted.clear();
        instanceReady = null;
    }

    @Override
    public void onChange(List<ResourceChange> changes) {
        if (!changes.isEmpty() && checkInstanceReady()) {
            bootWhenScriptsChanged();
        }
    }

    @Override
    public void onEvent(Event event) {
        EventType eventType = EventType.of(event.getName()).orElse(null);
        if (eventType == EventType.SCRIPT_SCHEDULER_BOOT) {
            bootOnDemand();
        }
    }

    public void bootOnDemand() {
        LOG.info("Automatic scripts booting on demand - job scheduling");
        unscheduleBoot();
        scheduleBoot();
        LOG.info("Automatic scripts booting on demand - job scheduled");
    }

    private void bootWhenInstanceUp() {
        LOG.info("Automatic scripts booting on instance up - job scheduling");
        unscheduleBoot();
        scheduleBoot();
        LOG.info("Automatic scripts booting on instance up - job scheduled");
    }

    private void bootWhenScriptsChanged() {
        LOG.info("Automatic scripts booting on script changes - job scheduling");
        unscheduleBoot();
        scheduleBoot();
        LOG.info("Automatic scripts booting on script changes - job scheduled");
    }

    private void unscheduleBoot() {
        Collection<ScheduledJobInfo> scheduledJobs = jobManager.getScheduledJobs(JOB_TOPIC, 0, (Map<String, Object>[]) null);
        for (ScheduledJobInfo scheduledJobInfo : scheduledJobs) {
            scheduledJobInfo.unschedule();
        }
    }

    private void scheduleBoot() {
        JobBuilder jobBuilder = jobManager.createJob(JOB_TOPIC);
        jobBuilder.properties(Map.of("type", JobType.BOOT.name()));
        JobBuilder.ScheduleBuilder scheduleBuilder = jobBuilder.schedule();
        scheduleBuilder.at(new Date(System.currentTimeMillis() + 1000));
        scheduleBuilder.add();
    }

    @Override
    public JobResult process(Job job) {
        String jobTypeValue = job.getProperty("type", String.class);
        try {
            JobType jobType = JobType.of(jobTypeValue);
            switch (jobType) {
                case BOOT:
                    bootJob();
                    break;
                case CRON:
                    String scriptPath = job.getProperty("scriptPath", String.class);
                    cronJob(scriptPath);
                    break;
            }
        } catch (IllegalArgumentException e) {
            LOG.error("Unknown job type: {}", jobTypeValue, e);
            return JobResult.FAILED;
        }
        return JobResult.OK;
    }

    private ScheduleResult determineSchedule(Script script, ResourceResolver resourceResolver) {
        try (ExecutionContext context =
                executor.createContext(ExecutionId.generate(), ExecutionMode.PARSE, script, resourceResolver)) {
            return executor.schedule(context);
        }
    }

    private void bootJob() {
        LOG.info("Automatic scripts booting - job started");
        unscheduleScripts();
        if (awaitInstanceHealthy(
                "Automatic scripts queueing and scheduling",
                config.healthCheckRetryCountBoot(),
                config.healthCheckRetryInterval())) {
            queueAndScheduleScripts();
        }
        LOG.info("Automatic scripts booting - job finished");
    }

    private boolean checkInstanceReady() {
        if (instanceReady == null) {
            if (InstanceType.CLOUD_CONTAINER.equals(instanceInfo.getType())) {
                try (ResourceResolver resourceResolver = ResolverUtils.contentResolver(resourceResolverFactory, null)) {
                    Repo repo = new Repo(resourceResolver);
                    instanceReady = repo.isCompositeNodeStore();
                } catch (LoginException e) {
                    LOG.error("Cannot access repository while checking instance readiness!", e);
                }
            } else {
                instanceReady = true;
            }
        }
        return instanceReady;
    }

    private void unscheduleScripts() {
        for (Map.Entry<String, ScheduledJobInfo> entry : scheduled.entrySet()) {
            String scriptPath = entry.getKey();
            ScheduledJobInfo scheduledJobInfo = entry.getValue();
            try {
                scheduledJobInfo.unschedule();
                LOG.debug("Cron schedule script '{}' unscheduled", scriptPath);
            } catch (Exception e) {
                LOG.error("Cron schedule script '{}' cannot be unscheduled!", scriptPath, e);
            }
        }
        scheduled.clear();
    }

    private void queueAndScheduleScripts() {
        try (ResourceResolver resourceResolver = ResolverUtils.contentResolver(resourceResolverFactory, null)) {
            ScriptRepository scriptRepository = new ScriptRepository(resourceResolver);
            scriptRepository.findAll(ScriptType.AUTOMATIC).forEach(script -> {
                ScheduleResult scheduleResult = determineSchedule(script, resourceResolver);
                if (scheduleResult.getExecution().getStatus() == ExecutionStatus.SUCCEEDED) {
                    Schedule schedule = scheduleResult.getSchedule();
                    if (schedule instanceof BootSchedule) {
                        queueBootScript(script, resourceResolver);
                    } else if (schedule instanceof CronSchedule) {
                        scheduleCronScript(script, (CronSchedule) schedule);
                    } else if (schedule instanceof NoneSchedule) {
                        LOG.info("Automatic script '{}' skipped due to schedule 'none'", script.getId());
                    } else {
                        LOG.info(
                                "Automatic script '{}' skipped due to schedule '{}' (unsupported)",
                                script.getId(),
                                schedule.getId());
                    }
                } else {
                    LOG.error("Automatic script schedule cannot be determined: {}", scheduleResult.getExecution());
                }
            });
        } catch (LoginException e) {
            LOG.error("Cannot access repository while determining automatic script schedules!", e);
        }
    }

    private void queueBootScript(Script script, ResourceResolver resourceResolver) {
        String checksum = ChecksumUtils.calculate(script.getContent());
        String previousChecksum = booted.get(script.getId());
        if (previousChecksum == null || !StringUtils.equals(previousChecksum, checksum)) {
            if (checkScript(script, resourceResolver)) {
                queueScript(script);
                booted.put(script.getId(), checksum);
                LOG.info("Boot script '{}' queued", script.getId());
            } else {
                LOG.info("Boot script '{}' not eligible for queueing!", script.getId());
            }
        }
    }

    private void scheduleCronScript(Script script, CronSchedule schedule) {
        if (StringUtils.isNotBlank(schedule.getExpression())) {
            JobBuilder jobBuilder = jobManager.createJob(JOB_TOPIC);
            jobBuilder.properties(Map.of("type", JobType.CRON.name(), "scriptPath", script.getPath()));
            JobBuilder.ScheduleBuilder scheduleBuilder = jobBuilder.schedule();
            scheduleBuilder.cron(schedule.getExpression());
            ScheduledJobInfo scheduledJobInfo = scheduleBuilder.add();
            scheduled.put(script.getPath(), scheduledJobInfo);
            LOG.info(
                    "Cron schedule script '{}' scheduled with expression '{}'",
                    script.getId(),
                    schedule.getExpression());
        } else {
            LOG.error("Cron schedule script '{}' not scheduled as no expression defined!", script.getId());
        }
    }

    private void cronJob(String scriptPath) {
        LOG.info("Cron schedule script '{}' - job started", scriptPath);
        if (awaitInstanceHealthy(
                String.format("Cron schedule script '%s' queueing", scriptPath),
                config.healthCheckRetryCountCron(),
                config.healthCheckRetryInterval())) {
            try (ResourceResolver resourceResolver = ResolverUtils.contentResolver(resourceResolverFactory, null)) {
                ScriptRepository scriptRepository = new ScriptRepository(resourceResolver);
                Script script = scriptRepository.read(scriptPath).orElse(null);
                if (script == null) {
                    LOG.error("Cron schedule script '{}' not found in repository!", scriptPath);
                } else {
                    if (checkScript(script, resourceResolver)) {
                        queueScript(script);
                    } else {
                        LOG.info("Cron schedule script '{}' not eligible for queueing!", scriptPath);
                    }
                }
            } catch (LoginException e) {
                LOG.error("Cannot access repository while queueing cron schedule script '{}'!", scriptPath, e);
            }
        }
        LOG.info("Cron schedule script '{}' - job finished", scriptPath);
    }

    private boolean checkScript(Script script, ResourceResolver resourceResolver) {
        try (ExecutionContext context =
                executor.createContext(ExecutionId.generate(), ExecutionMode.PARSE, script, resourceResolver)) {
            if (executor.isLocked(context)) {
                LOG.info("Script '{}' already locked!", script.getPath());
                return false;
            }

            long queueCurrentSize = executionQueue.getCurrentSize();
            long queueMaxSize = executionQueue.getMaxSize();
            if (queueCurrentSize >= queueMaxSize) {
                LOG.info(
                        "Script '{}' not queued because queue is full ({}/{})!",
                        script.getPath(),
                        queueCurrentSize,
                        queueMaxSize);
                return false;
            }

            Execution queued = executionQueue
                    .findByExecutableId(context.getExecutable().getId())
                    .orElse(null);
            if (queued != null) {
                LOG.info("Script '{}' already queued: {}", script.getPath(), queued);
                return false;
            }

            Execution checking = executor.check(context);
            if (checking.getStatus() != ExecutionStatus.SUCCEEDED) {
                LOG.info("Script '{}' checking not succeeded: {}", script.getPath(), checking);
                return false;
            }

            return true;
        }
    }

    private void queueScript(Script script) {
        String userId =
                StringUtils.defaultIfBlank(config.userImpersonationId(), ResolverUtils.Subservice.CONTENT.userId);
        executionQueue.submit(script, new ExecutionContextOptions(ExecutionMode.RUN, userId));
    }

    private boolean awaitInstanceHealthy(String operation, long retryMaxCount, long retryInterval) {
        HealthStatus healthStatus = null;
        long retryCount = 0;
        while (healthStatus == null || !healthStatus.getHealthy()) {
            if (retryCount >= retryMaxCount) {
                LOG.error(
                        "{} aborted due to unhealthy instance state after {} retries: {}",
                        operation,
                        retryMaxCount,
                        healthStatus);
                return false;
            }
            healthStatus = healthChecker.checkStatus();
            if (healthStatus.getHealthy()) {
                break;
            } else {
                LOG.warn("{} paused due to unhealthy instance state: {}", operation, healthStatus);
                try {
                    Thread.sleep(retryInterval); // wait before retrying
                    retryCount++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOG.error("{} interrupted!", operation, e);
                }
            }
        }
        if (retryCount > 0) {
            LOG.info("{} reached healthy instance state after {} retries: {}", operation, retryCount, healthStatus);
        } else {
            LOG.info("{} reached healthy instance state: {}", operation, healthStatus);
        }
        return true;
    }
}
