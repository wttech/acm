package dev.vml.es.acm.core.script;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.code.*;
import dev.vml.es.acm.core.code.schedule.BootSchedule;
import dev.vml.es.acm.core.code.schedule.CronSchedule;
import dev.vml.es.acm.core.code.schedule.NoneSchedule;
import dev.vml.es.acm.core.instance.HealthChecker;
import dev.vml.es.acm.core.osgi.InstanceInfo;
import dev.vml.es.acm.core.util.ChecksumUtils;
import dev.vml.es.acm.core.util.ResolverUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
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
        service = JobConsumer.class,
        immediate = true,
        property = {
            JobConsumer.PROPERTY_TOPICS + "=" + AcmConstants.CODE + "/boot"
        })
@Designate(ocd = AutomaticScriptBootJobConsumer.Config.class)
public class AutomaticScriptBootJobConsumer implements JobConsumer {

    @ObjectClassDefinition(
            name = "AEM Content Manager - Automatic Script Boot Job Consumer",
            description = "Consumes boot script execution jobs")
    public @interface Config {

        @AttributeDefinition(
                name = "User Impersonation ID",
                description =
                        "Controls who accesses the repository when scripts are automatically executed. If blank, the service user 'acm-content-service' is used.")
        String userImpersonationId() default "";

        @AttributeDefinition(
                name = "Health Check Retry Interval",
                description = "Interval in milliseconds to retry health check if instance is not healthy")
        long healthCheckRetryInterval() default 1000 * 10; // 10 seconds

        @AttributeDefinition(
                name = "Health Check Retry Count On Boot",
                description = "Maximum number of retries when checking instance health on boot script execution")
        long healthCheckRetryCountBoot() default 90; // * 10 seconds = 15 minutes
    }

    private static final Logger LOG = LoggerFactory.getLogger(AutomaticScriptBootJobConsumer.class);

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

    private final Map<String, String> booted = new ConcurrentHashMap<>();
    private final List<String> scheduled = new CopyOnWriteArrayList<>();

    private Config config;

    @Activate
    protected void activate(Config config) {
        this.config = config;
    }

    @Override
    public JobResult process(Job job) {
        LOG.info("Automatic scripts booting - job started");
        unscheduleScripts();
        
        if (ScriptJobUtils.awaitInstanceHealthy(
                healthChecker,
                "Automatic scripts queueing and scheduling",
                config.healthCheckRetryCountBoot(),
                config.healthCheckRetryInterval())) {
            queueAndScheduleScripts(job);
        }
        LOG.info("Automatic scripts booting - job finished");
        return JobResult.OK;
    }

    private void unscheduleScripts() {
        for (String scriptPath : scheduled) {
            try {
                Collection<ScheduledJobInfo> scheduledJobs = jobManager.getScheduledJobs(AcmConstants.CODE + "/cron", 0, (Map<String, Object>[]) null);
                for (ScheduledJobInfo scheduledJobInfo : scheduledJobs) {
                    if (scriptPath.equals(scheduledJobInfo.getJobTopic())) {
                        scheduledJobInfo.unschedule();
                    }
                }
            } catch (Exception e) {
                LOG.error("Cron schedule script '{}' cannot be unscheduled!", scriptPath, e);
            }
        }
        scheduled.clear();
    }

    private void queueAndScheduleScripts(Job job) {
        try (ResourceResolver resourceResolver = ResolverUtils.contentResolver(resourceResolverFactory, null)) {
            ScriptRepository scriptRepository = new ScriptRepository(resourceResolver);
            scriptRepository.findAll(ScriptType.AUTOMATIC).forEach(script -> {
                ScheduleResult scheduleResult = determineSchedule(script, resourceResolver);
                if (scheduleResult.getExecution().getStatus() == ExecutionStatus.SUCCEEDED) {
                    Schedule schedule = scheduleResult.getSchedule();
                    if (schedule instanceof BootSchedule) {
                        queueBootScript(script, resourceResolver, job);
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

    private ScheduleResult determineSchedule(Script script, ResourceResolver resourceResolver) {
        try (ExecutionContext context =
                executor.createContext(ExecutionId.generate(), ExecutionMode.PARSE, script, resourceResolver)) {
            return executor.schedule(context);
        }
    }

    private void queueBootScript(Script script, ResourceResolver resourceResolver, Job job) {
        String checksum = ChecksumUtils.calculate(script.getContent());
        String previousChecksum = booted.get(script.getId());
        if (previousChecksum == null || !StringUtils.equals(previousChecksum, checksum)) {
            if (ScriptJobUtils.checkScript(script, resourceResolver, executor, executionQueue)) {
                ScriptJobUtils.queueScript(script, config.userImpersonationId(), executionQueue);
                booted.put(script.getId(), checksum);
                LOG.info("Boot script '{}' queued", script.getId());
            } else {
                LOG.info("Boot script '{}' not eligible for queueing!", script.getId());
            }
        }
    }

    private void scheduleCronScript(Script script, CronSchedule schedule) {
        if (StringUtils.isNotBlank(schedule.getExpression())) {
            JobBuilder jobBuilder = jobManager.createJob(AcmConstants.CODE + "/cron");
            jobBuilder.properties(Map.of("scriptPath", script.getPath()));
            JobBuilder.ScheduleBuilder scheduleBuilder = jobBuilder.schedule();
            scheduleBuilder.cron(schedule.getExpression());
            scheduleBuilder.add();
            scheduled.add(script.getPath());
            LOG.info(
                    "Cron schedule script '{}' scheduled with expression '{}'",
                    script.getId(),
                    schedule.getExpression());
        } else {
            LOG.error("Cron schedule script '{}' not scheduled as no expression defined!", script.getId());
        }
    }
}