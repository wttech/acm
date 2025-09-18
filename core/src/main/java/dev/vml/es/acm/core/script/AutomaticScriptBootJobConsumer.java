package dev.vml.es.acm.core.script;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.code.*;
import dev.vml.es.acm.core.code.schedule.BootSchedule;
import dev.vml.es.acm.core.code.schedule.CronSchedule;
import dev.vml.es.acm.core.code.schedule.NoneSchedule;
import dev.vml.es.acm.core.instance.HealthChecker;
import dev.vml.es.acm.core.instance.HealthStatus;
import dev.vml.es.acm.core.osgi.InstanceInfo;
import dev.vml.es.acm.core.util.ChecksumUtils;
import dev.vml.es.acm.core.util.ResolverUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = JobConsumer.class,
        immediate = true,
        property = {
            JobConsumer.PROPERTY_TOPICS + "=" + AcmConstants.CODE + "/boot"
        })
public class AutomaticScriptBootJobConsumer implements JobConsumer {

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

    @Override
    public JobResult process(Job job) {
        LOG.info("Automatic scripts booting - job started");
        unscheduleScripts();
        
        Long healthCheckRetryCountBoot = job.getProperty("healthCheckRetryCountBoot", Long.class);
        Long healthCheckRetryInterval = job.getProperty("healthCheckRetryInterval", Long.class);
        
        if (healthCheckRetryCountBoot == null) healthCheckRetryCountBoot = 90L;
        if (healthCheckRetryInterval == null) healthCheckRetryInterval = 10000L;
        
        if (awaitInstanceHealthy(
                "Automatic scripts queueing and scheduling",
                healthCheckRetryCountBoot,
                healthCheckRetryInterval)) {
            queueAndScheduleScripts(job);
        }
        LOG.info("Automatic scripts booting - job finished");
        return JobResult.OK;
    }

    private void unscheduleScripts() {
        for (String scriptPath : scheduled) {
            try {
                java.util.Collection<ScheduledJobInfo> scheduledJobs = jobManager.getScheduledJobs(AcmConstants.CODE + "/cron", 0, (Map<String, Object>[]) null);
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
            if (checkScript(script, resourceResolver)) {
                queueScript(script, job);
                booted.put(script.getId(), checksum);
                LOG.info("Boot script '{}' queued", script.getId());
            } else {
                LOG.info("Boot script '{}' not eligible for queueing!", script.getId());
            }
        }
    }

    private void scheduleCronScript(Script script, CronSchedule schedule) {
        if (StringUtils.isNotBlank(schedule.getExpression())) {
            org.apache.sling.event.jobs.JobBuilder jobBuilder = jobManager.createJob(AcmConstants.CODE + "/cron");
            jobBuilder.properties(Map.of(
                "scriptPath", script.getPath(),
                "healthCheckRetryCountCron", 3L,
                "healthCheckRetryInterval", 10000L,
                "userImpersonationId", ""
            ));
            org.apache.sling.event.jobs.JobBuilder.ScheduleBuilder scheduleBuilder = jobBuilder.schedule();
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

    private void queueScript(Script script, Job job) {
        String userImpersonationId = job.getProperty("userImpersonationId", String.class);
        String userId = StringUtils.defaultIfBlank(userImpersonationId, ResolverUtils.Subservice.CONTENT.userId);
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