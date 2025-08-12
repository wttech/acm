package dev.vml.es.acm.core.script;

import dev.vml.es.acm.core.code.*;
import dev.vml.es.acm.core.code.schedule.BootSchedule;
import dev.vml.es.acm.core.code.schedule.CronSchedule;
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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
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
public class AutomaticScriptScheduler implements ResourceChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(AutomaticScriptScheduler.class);

    private static final long INSTANCE_HEALTHY_INTERVAL = 1000;

    private static final long INSTANCE_HEALTHY_RETRIES = 60 * 30; // 30 minutes

    private static final String BOOT_JOB_NAME = "boot";

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

    @Activate
    protected void activate() {
        if (checkInstanceReady()) {
            bootWhenInstanceUp();
        }
    }

    @Deactivate
    protected void deactivate() {
        unscheduleScripts();
    }

    @Override
    public void onChange(List<ResourceChange> changes) {
        if (!changes.isEmpty() && checkInstanceReady()) {
            bootWhenScriptsChanged();
        }
    }

    private void bootWhenInstanceUp() {
        LOG.info("Automatic scripts booting on instance up");
        scheduleBoot();
    }

    private void bootWhenScriptsChanged() {
        LOG.info("Automatic scripts booting on script changes");
        scheduleBoot();
    }

    private void scheduleBoot() {
        scheduler.unschedule(BOOT_JOB_NAME);
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
            if (awaitInstanceHealthy()) {
                bootOrScheduleScripts();
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

    private boolean awaitInstanceHealthy() {
        HealthStatus healthStatus = null;
        long retries = 0;
        while (healthStatus == null || !healthStatus.isHealthy()) {
            if (retries >= INSTANCE_HEALTHY_RETRIES) {
                LOG.error(
                        "Automatic scripts booting failed after {} retries due to health status: {}",
                        INSTANCE_HEALTHY_RETRIES,
                        healthStatus);
                return false;
            }
            healthStatus = healthChecker.checkStatus();
            if (healthStatus.isHealthy()) {
                break;
            } else {
                LOG.warn("Automatic scripts booting paused due to health status: {}", healthStatus);
                try {
                    Thread.sleep(INSTANCE_HEALTHY_INTERVAL); // wait before retrying
                    retries++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOG.error("Automatic scripts booting interrupted!", e);
                }
            }
        }
        return true;
    }

    private void unscheduleScripts() {
        scheduler.unschedule(BOOT_JOB_NAME);
        for (String scriptPath : scheduled) {
            try {
                scheduler.unschedule(scriptPath);
            } catch (Exception e) {
                LOG.error("Cron schedule script '{}' cannot be unscheduled!", scriptPath, e);
            }
        }
        scheduled.clear();
    }

    private void bootOrScheduleScripts() {
        try (ResourceResolver resourceResolver = ResourceUtils.contentResolver(resourceResolverFactory, null)) {
            ScriptRepository scriptRepository = new ScriptRepository(resourceResolver);
            scriptRepository.findAll(ScriptType.AUTOMATIC).forEach(script -> {
                ScheduleResult scheduleResult = determineSchedule(script, resourceResolver);
                if (scheduleResult.getExecution().getStatus() == ExecutionStatus.SUCCEEDED) {
                    Schedule schedule = scheduleResult.getSchedule();
                    if (schedule instanceof BootSchedule) {
                        bootScript(script, resourceResolver);
                    } else if (schedule instanceof CronSchedule) {
                        scheduleScript(script, (CronSchedule) schedule);
                    }
                } else {
                    LOG.error("Automatic script schedule cannot be determined: {}", scheduleResult.getExecution());
                }
            });
        } catch (LoginException e) {
            LOG.error("Cannot access repository while determining automatic script schedules!", e);
        }
    }

    private void bootScript(Script script, ResourceResolver resourceResolver) {
        String checksum = ChecksumUtils.calculate(script.getContent());
        String previousChecksum = booted.get(script.getId());
        if (previousChecksum == null || StringUtils.equals(previousChecksum, checksum)) {
            if (checkScript(script, resourceResolver)) {
                queueScript(script);
                booted.put(script.getId(), checksum);
            } else {
                LOG.info("Boot script '{}' is not ready for queueing!", script.getPath());
            }
        }
    }

    private void scheduleScript(Script script, CronSchedule schedule) {
        if (StringUtils.isNotBlank(schedule.getExpression())) {
            scheduler.schedule(
                    cronJob(script.getPath()),
                    configureScheduleOptions(script.getPath(), scheduler.EXPR(schedule.getExpression())));
            scheduled.add(script.getPath());
        } else {
            LOG.error("Cron schedule for script '{}' has no cron expression defined!", script.getPath());
        }
    }

    private boolean checkScript(Script script, ResourceResolver resourceResolver) {
        try (ExecutionContext context =
                executor.createContext(ExecutionId.generate(), ExecutionMode.PARSE, script, resourceResolver)) {
            if (executor.isLocked(context)) {
                LOG.info("Script '{}' is already locked!", script.getPath());
                return false;
            }
            Execution executionQueued = executionQueue
                    .findByExecutableId(context.getExecutable().getId())
                    .orElse(null);
            if (executionQueued != null) {
                LOG.info("Script '{}' is already queued: {}", script.getPath(), executionQueued);
                return false;
            }
            Execution executionChecking = executor.check(context);
            if (executionChecking.getStatus() != ExecutionStatus.SUCCEEDED) {
                LOG.info("Script '{}' cannot run: {}", script.getPath(), executionChecking);
                return false;
            }
            return true;
        }
    }

    private void queueScript(Script script) {
        executionQueue.submit(script, new ExecutionContextOptions(ExecutionMode.RUN, null));
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
                        LOG.warn("Cron schedule script '{}' is not ready for queueing!", scriptPath);
                    }
                }
            } catch (LoginException e) {
                LOG.error("Cannot access repository while queueing cron schedule script '{}'!", scriptPath, e);
            }
        };
    }
}
