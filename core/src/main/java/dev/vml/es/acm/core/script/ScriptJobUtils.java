package dev.vml.es.acm.core.script;

import dev.vml.es.acm.core.code.*;
import dev.vml.es.acm.core.instance.HealthChecker;
import dev.vml.es.acm.core.instance.HealthStatus;
import dev.vml.es.acm.core.util.ResolverUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared utility class for script job consumers containing common functionality.
 */
public class ScriptJobUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ScriptJobUtils.class);

    private ScriptJobUtils() {
        // Utility class
    }

    public static boolean awaitInstanceHealthy(HealthChecker healthChecker, String operation, long retryMaxCount, long retryInterval) {
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

    public static boolean checkScript(Script script, ResourceResolver resourceResolver, Executor executor, ExecutionQueue executionQueue) {
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

    public static void queueScript(Script script, String userImpersonationId, ExecutionQueue executionQueue) {
        String userId = StringUtils.defaultIfBlank(userImpersonationId, ResolverUtils.Subservice.CONTENT.userId);
        executionQueue.submit(script, new ExecutionContextOptions(ExecutionMode.RUN, userId));
    }
}