package dev.vml.es.acm.core.script;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.code.*;
import dev.vml.es.acm.core.code.schedule.BootSchedule;
import dev.vml.es.acm.core.code.schedule.CronSchedule;
import dev.vml.es.acm.core.code.schedule.NoneSchedule;
import dev.vml.es.acm.core.instance.HealthChecker;
import dev.vml.es.acm.core.instance.HealthStatus;
import dev.vml.es.acm.core.osgi.InstanceInfo;
import dev.vml.es.acm.core.osgi.InstanceType;
import dev.vml.es.acm.core.repo.Repo;
import dev.vml.es.acm.core.util.ChecksumUtils;
import dev.vml.es.acm.core.util.ResourceUtils;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.commons.scheduler.Job;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = ResourceChangeListener.class,
        immediate = true,
        property = {
            ResourceChangeListener.PATHS + "=glob:" + ScriptRepository.ROOT + "/automatic/**/*.groovy",
            ResourceChangeListener.CHANGES + "=ADDED",
            ResourceChangeListener.CHANGES + "=CHANGED",
            ResourceChangeListener.CHANGES + "=REMOVED"
        })
@Designate(ocd = AutomaticScriptScheduler.Config.class)
public class AutomaticScriptScheduler implements ResourceChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(AutomaticScriptScheduler.class);

    private static final String BOOT_JOB_NAME = AcmConstants.CODE + "-boot";

    @ObjectClassDefinition(
            name = "AEM Content Manager - Automatic Script Scheduler",
            description = "Schedules automatic scripts on instance up and script changes")
    public @interface Config {

        @AttributeDefinition(
                name = "User Impersonation ID",
                description =
                        "Controls who accesses the repository when scripts are automatically executed. If blank, the service user 'acm-content-service' is used.")
        String userImpersonationId();

        @AttributeDefinition(
                name = "Health Check Interval",
                description = "Interval in milliseconds to retry health check if instance is not healthy")
        long healthRetryInterval() default 1000 * 10; // 10 seconds

        @AttributeDefinition(
                name = "Health Check Max Count - Boot",
                description =
                        "Maximum number of retries after booting instance or installing package with automatic scripts")
        long healthRetryMaxCountBoot() default 90; // 90 times * 10 seconds = 15 minutes
    }

    private Boolean instanceReady;

    private final Map<String, String> booted = new ConcurrentHashMap<>();

    private final List<String> scheduled = new CopyOnWriteArrayList<>();

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private InstanceInfo instanceInfo;

    @Reference
    private Executor executor;

    @Reference
    private Scheduler scheduler;

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

    private void bootWhenInstanceUp() {
        LOG.info("Automatic scripts booting on instance up");
        unscheduleBoot();
        scheduleBoot();
    }

    private void bootWhenScriptsChanged() {
        LOG.info("Automatic scripts booting on script changes");
        unscheduleBoot();
        scheduleBoot();
    }

    private void unscheduleBoot() {
        scheduler.unschedule(BOOT_JOB_NAME);
    }

    private void scheduleBoot() {
        scheduler.schedule(bootJob(), configureScheduleOptions(BOOT_JOB_NAME, scheduler.NOW()));
    }

    private ScheduleOptions configureScheduleOptions(String name, ScheduleOptions options) {
        options.name(name);
        options.onLeaderOnly(true);
        options.canRunConcurrently(false);
        options.onSingleInstanceOnly(true);
        return options;
    }

    private ScheduleResult determineSchedule(Script script, ResourceResolver resourceResolver) {
        try (ExecutionContext context =
                executor.createContext(ExecutionId.generate(), ExecutionMode.PARSE, script, resourceResolver)) {
            return executor.schedule(context);
        }
    }

    private Job bootJob() {
        return context -> {
            unscheduleScripts();
            if (awaitInstanceHealthy(
                    "Automatic scripts queueing and scheduling",
                    config.healthRetryMaxCountBoot(),
                    config.healthRetryInterval())) {
                queueAndScheduleScripts();
            }
        };
    }

    private boolean checkInstanceReady() {
        if (instanceReady == null) {
            if (InstanceType.CLOUD_CONTAINER.equals(instanceInfo.getType())) {
                try (ResourceResolver resourceResolver = ResourceUtils.contentResolver(resourceResolverFactory, null)) {
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
        for (String scriptPath : scheduled) {
            try {
                scheduler.unschedule(scriptPath);
            } catch (Exception e) {
                LOG.error("Cron schedule script '{}' cannot be unscheduled!", scriptPath, e);
            }
        }
        scheduled.clear();
    }

    private void queueAndScheduleScripts() {
        try (ResourceResolver resourceResolver = ResourceUtils.contentResolver(resourceResolverFactory, null)) {
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
            scheduler.schedule(
                    cronJob(script.getPath()),
                    configureScheduleOptions(script.getPath(), scheduler.EXPR(schedule.getExpression())));
            scheduled.add(script.getPath());
            LOG.info(
                    "Cron schedule script '{}' scheduled with expression '{}'",
                    script.getId(),
                    schedule.getExpression());
        } else {
            LOG.error("Cron schedule script '{}' not scheduled as no expression defined!", script.getId());
        }
    }

    private Job cronJob(String scriptPath) {
        return context -> {
            try (ResourceResolver resourceResolver = ResourceUtils.contentResolver(resourceResolverFactory, null)) {
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
        };
    }

    private boolean checkScript(Script script, ResourceResolver resourceResolver) {
        try (ExecutionContext context =
                executor.createContext(ExecutionId.generate(), ExecutionMode.PARSE, script, resourceResolver)) {
            if (executor.isLocked(context)) {
                LOG.info("Script '{}' already locked!", script.getPath());
                return false;
            }
            Execution executionQueued = executionQueue
                    .findByExecutableId(context.getExecutable().getId())
                    .orElse(null);
            if (executionQueued != null) {
                LOG.info("Script '{}' already queued: {}", script.getPath(), executionQueued);
                return false;
            }
            Execution executionChecking = executor.check(context);
            if (executionChecking.getStatus() != ExecutionStatus.SUCCEEDED) {
                LOG.info("Script '{}' checking not succeeded: {}", script.getPath(), executionChecking);
                return false;
            }
            return true;
        }
    }

    private void queueScript(Script script) {
        String userId =
                StringUtils.defaultIfBlank(config.userImpersonationId(), ResourceUtils.Subservice.CONTENT.userId);
        executionQueue.submit(script, new ExecutionContextOptions(ExecutionMode.RUN, userId));
    }

    private boolean awaitInstanceHealthy(String operation, long retryMaxCount, long retryInterval) {
        HealthStatus healthStatus = null;
        long retryCount = 0;
        while (healthStatus == null || !healthStatus.isHealthy()) {
            if (retryCount >= retryMaxCount) {
                LOG.error(
                        "{} not reached healthy instance state after {} retries: {}",
                        operation,
                        retryMaxCount,
                        healthStatus);
                return false;
            }
            healthStatus = healthChecker.checkStatus();
            if (healthStatus.isHealthy()) {
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
