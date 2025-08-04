package dev.vml.es.acm.core.script;

import dev.vml.es.acm.core.code.ExecutionContextOptions;
import dev.vml.es.acm.core.code.ExecutionMode;
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

    private static final String BOOT_JOB_NAME = "boot";

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
            scriptRepository.findAll(ScriptType.SCHEDULE).forEach(script -> scheduleScript(script.getPath(), resourceResolver));
            scheduler.schedule(bootJob(), configureSchedule(BOOT_JOB_NAME, scheduler.NOW()));
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
    public void onChange(List<ResourceChange> changes) {
        if (changes.isEmpty()) {
            return;
        }
        try (ResourceResolver resourceResolver = ResourceUtils.contentResolver(resourceResolverFactory, null)) {
            boolean reboot = false;
            for (ResourceChange change : changes) {
                String scriptPath = change.getPath();

                unscheduleScript(scriptPath);

                switch (change.getType()) {
                    case ADDED:
                    case CHANGED:
                        scheduleScript(scriptPath, resourceResolver);
                        reboot = true;
                        break;
                }
            }
            if (reboot) {
                LOG.info("Boot scripts changed, reboot required");
                scheduler.unschedule(BOOT_JOB_NAME);
                scheduler.schedule(bootJob(), configureSchedule(BOOT_JOB_NAME, scheduler.NOW()));
            } else {
                LOG.info("Boot scripts not changed, reboot skipped");
            }
        } catch (LoginException e) {
            LOG.error("Cannot access repository while processing automatic script changes!", e);
        }
    }

    private void unscheduleScript(String scriptPath) {
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

    private void scheduleScript(String scriptPath, ResourceResolver resourceResolver) {
        ScriptSchedule schedule = determineSchedule(scriptPath, resourceResolver);
        if (schedule instanceof BootSchedule) {
            schedules.put(scriptPath, schedule);
        } else if (schedule instanceof CronSchedule) {
            CronSchedule cron = (CronSchedule) schedule;
            if (StringUtils.isNotBlank(cron.getExpression())) {
                scheduler.schedule(cronJob(scriptPath), configureSchedule(scriptPath, scheduler.EXPR(cron.getExpression())));
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
                    LOG.warn("Boot scripts paused due to health status: {}", healthStatus);
                    try {
                        Thread.sleep(1000); // wait before retrying
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LOG.error("Boot scripts queueing interrupted!", e);
                    }
                }
            }
            try (ResourceResolver resourceResolver = ResourceUtils.contentResolver(resourceResolverFactory, null)) {
                ScriptRepository scriptRepository = new ScriptRepository(resourceResolver);
                for (Map.Entry<String, ScriptSchedule> e : schedules.entrySet()) {
                    if (e.getValue() instanceof BootSchedule) {
                        String scriptPath = e.getKey();
                        BootSchedule boot = (BootSchedule) e.getValue();
                        Script script = scriptRepository.read(scriptPath).orElse(null);
                        if (script == null) {
                            LOG.error("Boot script '{}' cannot be read while queueing!", scriptPath);
                        } else if (!boot.getDone().get()) {
                            if (!checkScript(script)) {
                                LOG.info("Boot script '{}' is not ready for queueing!", scriptPath);
                            } else {
                                queueScript(script);
                                boot.getDone().set(true);
                            }
                        }
                    }
                }
            } catch (LoginException e) {
                LOG.error("Cannot access repository while boot scripts queueing!", e);
            }
        };
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
                    LOG.warn("Script '{}' not found in repository, not queueing!", scriptPath);
                } else {
                    if (checkScript(script)) {
                        queueScript(script);
                    } else {
                        LOG.debug("Script '{}' is not ready for execution, not queueing!", scriptPath);
                    }
                }
            } catch (LoginException e) {
                LOG.error("Cannot access repository while queueing cron script execution '{}'!", scriptPath, e);
            }
        };
    }
}
