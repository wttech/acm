package dev.vml.es.acm.core.script;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.code.*;
import dev.vml.es.acm.core.instance.HealthChecker;
import dev.vml.es.acm.core.instance.HealthStatus;
import dev.vml.es.acm.core.util.ResolverUtils;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = JobConsumer.class,
        immediate = true,
        property = {
            JobConsumer.PROPERTY_TOPICS + "=" + AcmConstants.CODE + "/cron"
        })
public class AutomaticScriptCronJobConsumer implements JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(AutomaticScriptCronJobConsumer.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Executor executor;

    @Reference
    private HealthChecker healthChecker;

    @Reference
    private ExecutionQueue executionQueue;

    @Override
    public JobResult process(Job job) {
        String scriptPath = job.getProperty("scriptPath", String.class);
        LOG.info("Cron schedule script '{}' - job started", scriptPath);
        
        Long healthCheckRetryCountCron = job.getProperty("healthCheckRetryCountCron", Long.class);
        Long healthCheckRetryInterval = job.getProperty("healthCheckRetryInterval", Long.class);
        String userImpersonationId = job.getProperty("userImpersonationId", String.class);
        
        if (healthCheckRetryCountCron == null) healthCheckRetryCountCron = 3L;
        if (healthCheckRetryInterval == null) healthCheckRetryInterval = 10000L;
        
        if (awaitInstanceHealthy(
                String.format("Cron schedule script '%s' queueing", scriptPath),
                healthCheckRetryCountCron,
                healthCheckRetryInterval)) {
            try (ResourceResolver resourceResolver = ResolverUtils.contentResolver(resourceResolverFactory, null)) {
                ScriptRepository scriptRepository = new ScriptRepository(resourceResolver);
                Script script = scriptRepository.read(scriptPath).orElse(null);
                if (script == null) {
                    LOG.error("Cron schedule script '{}' not found in repository!", scriptPath);
                } else {
                    if (checkScript(script, resourceResolver)) {
                        queueScript(script, userImpersonationId);
                    } else {
                        LOG.info("Cron schedule script '{}' not eligible for queueing!", scriptPath);
                    }
                }
            } catch (LoginException e) {
                LOG.error("Cannot access repository while queueing cron schedule script '{}'!", scriptPath, e);
            }
        }
        LOG.info("Cron schedule script '{}' - job finished", scriptPath);
        return JobResult.OK;
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

    private void queueScript(Script script, String userImpersonationId) {
        String userId = org.apache.commons.lang3.StringUtils.defaultIfBlank(userImpersonationId, ResolverUtils.Subservice.CONTENT.userId);
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