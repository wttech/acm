package dev.vml.es.acm.core.script;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.event.Event;
import dev.vml.es.acm.core.event.EventListener;
import dev.vml.es.acm.core.event.EventType;
import dev.vml.es.acm.core.osgi.InstanceInfo;
import dev.vml.es.acm.core.osgi.InstanceType;
import dev.vml.es.acm.core.repo.Repo;
import dev.vml.es.acm.core.util.ResolverUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = {ResourceChangeListener.class, EventListener.class},
        immediate = true,
        property = {
            ResourceChangeListener.PATHS + "=glob:" + ScriptRepository.ROOT + "/automatic/**/*.groovy",
            ResourceChangeListener.CHANGES + "=ADDED",
            ResourceChangeListener.CHANGES + "=CHANGED",
            ResourceChangeListener.CHANGES + "=REMOVED"
        })
@Designate(ocd = AutomaticScriptScheduler.Config.class)
public class AutomaticScriptScheduler implements ResourceChangeListener, EventListener {

    private static final Logger LOG = LoggerFactory.getLogger(AutomaticScriptScheduler.class);

    private static final String BOOT_JOB_TOPIC = AcmConstants.CODE + "/boot";

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

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private InstanceInfo instanceInfo;

    @Reference
    private JobManager jobManager;

    private Config config;

    @Activate
    protected void activate(Config config) {
        this.config = config;

        new Date(System.currentTimeMillis() + 1000);

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

    // TODO on AEMaaCS scheduler refuses to schedule job during activate
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
        java.util.Collection<ScheduledJobInfo> scheduledJobs = jobManager.getScheduledJobs(BOOT_JOB_TOPIC, 0, (Map<String, Object>[]) null);
        for (ScheduledJobInfo scheduledJobInfo : scheduledJobs) {
            scheduledJobInfo.unschedule();
        }
    }

    private void scheduleBoot() {
        org.apache.sling.event.jobs.JobBuilder jobBuilder = jobManager.createJob(BOOT_JOB_TOPIC);
        jobBuilder.properties(Map.of(
            "healthCheckRetryCountBoot", config.healthCheckRetryCountBoot(),
            "healthCheckRetryInterval", config.healthCheckRetryInterval(),
            "userImpersonationId", config.userImpersonationId()
        ));
        org.apache.sling.event.jobs.JobBuilder.ScheduleBuilder scheduleBuilder = jobBuilder.schedule();
        scheduleBuilder.at(new Date(System.currentTimeMillis() + 1000));
        scheduleBuilder.add();
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
}
