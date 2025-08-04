package dev.vml.es.acm.core.script;

import dev.vml.es.acm.core.code.ExecutionQueue;
import dev.vml.es.acm.core.instance.HealthChecker;
import dev.vml.es.acm.core.instance.HealthStatus;
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
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private final Map<String, ScriptSchedule> schedules = new ConcurrentHashMap<>();

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Scheduler scheduler;

    @Reference
    private HealthChecker healthChecker;

    @Reference
    private ExecutionQueue executionQueue;

    @Activate
    @Modified
    protected void activate() {
        try (ResourceResolver resourceResolver = ResourceUtils.contentResolver(resourceResolverFactory, null)) {
            ScriptRepository scriptRepository = new ScriptRepository(resourceResolver);
            scriptRepository.findAll(ScriptType.SCHEDULE).forEach(script -> schedule(script.getPath(), resourceResolver));
            scheduler.schedule(bootJob(), configureSchedule("boot", scheduler.NOW()));
        } catch (LoginException e) {
            LOG.error("Cannot access repository while starting automatic script scheduler!", e);
        }
    }

    private ScheduleOptions configureSchedule(String name, ScheduleOptions options) {
        options.name(name);
        options.onLeaderOnly(true);
        options.canRunConcurrently(false);
        options.onSingleInstanceOnly(true);
        return options;
    }

    @Override
    public void onChange(@NotNull List<ResourceChange> changes) {
        if (changes.isEmpty()) {
            return;
        }
        try (ResourceResolver resourceResolver = ResourceUtils.contentResolver(resourceResolverFactory, null)) {
            for (ResourceChange change : changes) {
                String scriptPath = change.getPath();
                unschedule(scriptPath);
                switch (change.getType()) {
                    case ADDED:
                    case CHANGED:
                        schedule(scriptPath, resourceResolver);
                        // TODO is non-cron schedule is here then do another boot schedule?
                        break;
                }
            }
        } catch (LoginException e) {
            LOG.error("Cannot access repository while processing automatic script changes!", e);
        }
    }

    private void unschedule(String scriptPath) {
        ScriptSchedule schedule = schedules.get(scriptPath);
        if (schedule == null) {
            return;
        }
        if (schedule instanceof BootSchedule) {
            schedules.remove(scriptPath);
        } else if (schedule instanceof CronSchedule) {
            scheduler.unschedule(scriptPath);
            schedules.remove(scriptPath);
        } else {
            throw new IllegalStateException(String.format("Schedule '%s' for script '%s' is not supported!", schedule.getId(), scriptPath));
        }
    }

    private void schedule(String scriptPath, ResourceResolver resourceResolver) {
        ScriptSchedule schedule = determineSchedule(scriptPath, resourceResolver);
        if (schedule instanceof BootSchedule) {
            schedules.put(scriptPath, schedule);
        } else if (schedule instanceof CronSchedule) {
            CronSchedule cron = (CronSchedule) schedule;
            if (StringUtils.isNotBlank(cron.getExpression())) {
                scheduler.schedule(cronJob(), configureSchedule(scriptPath, scheduler.EXPR(cron.getExpression())));
                schedules.put(scriptPath, schedule);
            } else {
                LOG.error("Schedule '{}' for script '{}' has no cron expression defined!", schedule.getId(), scriptPath);
            }
        } else {
            throw new IllegalStateException(String.format("Schedule '%s' for script '%s' is not supported!", schedule.getId(), scriptPath));
        }
    }

    private ScriptSchedule determineSchedule(String scriptPath, ResourceResolver resourceResolver) {
        return new BootSchedule(); // call script's scheduleRun() method
    }

    private Job bootJob() {
        return context -> {
            HealthStatus healthStatus = null;
            while (healthStatus == null || !healthStatus.isHealthy()) {
                healthStatus = healthChecker.checkStatus();
                if (healthStatus.isHealthy()) {
                    break;
                } else {
                    LOG.warn("Boot script scheduler is paused due to health status: {}", healthStatus);
                    try {
                        Thread.sleep(1000); // wait before retrying
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LOG.error("Boot script scheduler interrupted!", e);
                    }
                }
            }
            LOG.info("Boot script scheduler is starting...");
            for (Map.Entry<String, ScriptSchedule> entry : schedules.entrySet()) { // TODO do not boot twice same scripts
                if (entry.getValue() instanceof BootSchedule) {
                    // TODO executionQueue.submit()
                }
            }
        };
    }

    private Job cronJob() {
        return context -> {
            // TODO ...
        };
    }
}
