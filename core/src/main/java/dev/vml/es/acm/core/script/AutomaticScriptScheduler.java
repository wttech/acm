package dev.vml.es.acm.core.script;

import dev.vml.es.acm.core.code.ExecutionContextOptions;
import dev.vml.es.acm.core.code.ExecutionMode;
import dev.vml.es.acm.core.code.ExecutionQueue;
import dev.vml.es.acm.core.instance.HealthChecker;
import dev.vml.es.acm.core.instance.HealthStatus;
import dev.vml.es.acm.core.repo.Repo;
import dev.vml.es.acm.core.script.schedule.BootSchedule;
import dev.vml.es.acm.core.script.schedule.CronSchedule;
import dev.vml.es.acm.core.util.ResourceUtils;
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
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@Component(
        service = ResourceChangeListener.class,
        immediate = true,
        property = {
                ResourceChangeListener.PATHS + "=glob:"+ScriptRepository.ROOT+"/automatic/**/*.groovy",
                ResourceChangeListener.CHANGES + "=ADDED",
                ResourceChangeListener.CHANGES + "=CHANGED",
                ResourceChangeListener.CHANGES + "=REMOVED"
        }
)
public class AutomaticScriptScheduler implements ResourceChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(AutomaticScriptScheduler.class);

    private static final String BOOT_JOB_NAME = "boot";

    private boolean instanceReady = false;

    private final Map<String, AtomicBoolean> booted = new ConcurrentHashMap<>();

    private final List<String> scheduled = new CopyOnWriteArrayList<>();

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Scheduler scheduler;

    @Reference
    private HealthChecker healthChecker;

    @Reference
    private ExecutionQueue executionQueue;

    @Activate
    protected void activate() {
        checkInstanceReady();
        bootWhenInstanceUp();
    }

    private void checkInstanceReady() {
        try (ResourceResolver resourceResolver = ResourceUtils.contentResolver(resourceResolverFactory, null)) {
            Repo repo = new Repo(resourceResolver);
            this.instanceReady = repo.isCompositeNodeStore();
        } catch (LoginException e) {
            LOG.error("Cannot access repository while processing automatic script changes!", e);
            this.instanceReady = false;
        }
    }

    @Override
    public void onChange(List<ResourceChange> changes) {
        if (!instanceReady || changes.isEmpty()) {
            return;
        }
        bootWhenScriptsChanged();
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

    private ScriptSchedule determineSchedule(String scriptPath, ResourceResolver resourceResolver) {
        return new BootSchedule(); // TODO call script's scheduleRun() method
    }

    private Job bootJob() {
        return context -> {
            unscheduleScripts();
            awaitInstanceHealthy();
            bootOrScheduleScripts();
        };
    }

    private void awaitInstanceHealthy() {
        HealthStatus healthStatus = null;
        while (healthStatus == null || !healthStatus.isHealthy()) {
            healthStatus = healthChecker.checkStatus();
            if (healthStatus.isHealthy()) {
                break;
            } else {
                LOG.warn("Automatic scripts booting paused due to health status: {}", healthStatus);
                try {
                    Thread.sleep(1000); // wait before retrying
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOG.error("Automatic scripts booting interrupted!", e);
                }
            }
        }
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

    private void bootOrScheduleScripts() {
        try (ResourceResolver resourceResolver = ResourceUtils.contentResolver(resourceResolverFactory, null)) {
            ScriptRepository scriptRepository = new ScriptRepository(resourceResolver);
            scriptRepository.findAll(ScriptType.BOOT).forEach(script -> {
                ScriptSchedule schedule = determineSchedule(script.getPath(), resourceResolver);
                if (schedule instanceof BootSchedule) {
                    bootScript(script);
                } else if (schedule instanceof CronSchedule) {
                    scheduleScript(script, (CronSchedule) schedule);
                }
            });
        } catch (LoginException e) {
            LOG.error("Cannot access repository while automatic scripts booting!", e);
        }
    }

    // TODO what if script changed and was booted
    private void bootScript(Script script) {
        AtomicBoolean booted = this.booted.computeIfAbsent(script.getId(), s -> new AtomicBoolean(false));
        if (!booted.get()) {
            if (!checkScript(script)) {
                LOG.info("Boot script '{}' is not ready for queueing!", script.getPath());
            } else {
                queueScript(script);
                booted.set(true);
            }
        }
    }

    private void scheduleScript(Script script, CronSchedule schedule) {
        if (StringUtils.isNotBlank(schedule.getExpression())) {
            scheduler.schedule(cronJob(script.getPath()), configureScheduleOptions(script.getPath(), scheduler.EXPR(schedule.getExpression())));
            scheduled.add(script.getPath());
        } else {
            LOG.error("Cron schedule for script '{}' has no cron expression defined!", script.getPath());
        }
    }

    private boolean checkScript(Script script) {
        return false;
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
                    if (checkScript(script)) {
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
