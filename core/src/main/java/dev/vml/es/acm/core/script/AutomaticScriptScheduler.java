package dev.vml.es.acm.core.script;

import dev.vml.es.acm.core.script.schedule.BootSchedule;
import dev.vml.es.acm.core.script.schedule.CronSchedule;
import dev.vml.es.acm.core.util.ResourceUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.commons.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
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
                        break;
                }
            }
        } catch (LoginException e) {
            LOG.error("Cannot access repository while processing automatic script changes!", e);
        }
    }

    private void unschedule(String scriptPath) {
        ScriptSchedule schedule = schedules.get(scriptPath);
        if (schedule != null) {
            scheduler.unschedule(scriptPath);
        }
    }

    private void schedule(String scriptPath, ResourceResolver resourceResolver) {
        ScriptSchedule schedule = determineSchedule(scriptPath, resourceResolver);
        if (schedule instanceof BootSchedule) {
            schedules.put(scriptPath, schedule);
        } else if (schedule instanceof CronSchedule) {
            CronSchedule cron = (CronSchedule) schedule;
            if (StringUtils.isNotBlank(cron.getExpression())) {
                scheduler.schedule(scriptPath, scheduler.EXPR(cron.getExpression()));
                schedules.put(scriptPath, schedule);
            } else {
                LOG.error("No cron expression provided for script: {}", scriptPath);
            }
        } else {
            LOG.error("Unsupported schedule type for script: {}", scriptPath);
        }
    }

    private ScriptSchedule determineSchedule(String scriptPath, ResourceResolver resourceResolver) {
        return new BootSchedule(); // call script's scheduleRun() method
    }
}
