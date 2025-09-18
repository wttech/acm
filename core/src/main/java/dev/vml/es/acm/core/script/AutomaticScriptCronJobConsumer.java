package dev.vml.es.acm.core.script;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.code.*;
import dev.vml.es.acm.core.instance.HealthChecker;
import dev.vml.es.acm.core.util.ResolverUtils;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
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
            JobConsumer.PROPERTY_TOPICS + "=" + AcmConstants.CODE + "/cron"
        })
@Designate(ocd = AutomaticScriptCronJobConsumer.Config.class)
public class AutomaticScriptCronJobConsumer implements JobConsumer {

    @ObjectClassDefinition(
            name = "AEM Content Manager - Automatic Script Cron Job Consumer",
            description = "Consumes cron scheduled script execution jobs")
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
                name = "Health Check Retry Count On Cron Schedule",
                description =
                        "Maximum number of retries when checking instance health on cron schedule script execution")
        long healthCheckRetryCountCron() default 3; // * 10 seconds = 30 seconds
    }

    private static final Logger LOG = LoggerFactory.getLogger(AutomaticScriptCronJobConsumer.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Executor executor;

    @Reference
    private HealthChecker healthChecker;

    @Reference
    private ExecutionQueue executionQueue;

    private Config config;

    @Activate
    protected void activate(Config config) {
        this.config = config;
    }

    @Override
    public JobResult process(Job job) {
        String scriptPath = job.getProperty("scriptPath", String.class);
        LOG.info("Cron schedule script '{}' - job started", scriptPath);
        
        if (ScriptJobUtils.awaitInstanceHealthy(
                healthChecker,
                String.format("Cron schedule script '%s' queueing", scriptPath),
                config.healthCheckRetryCountCron(),
                config.healthCheckRetryInterval())) {
            try (ResourceResolver resourceResolver = ResolverUtils.contentResolver(resourceResolverFactory, null)) {
                ScriptRepository scriptRepository = new ScriptRepository(resourceResolver);
                Script script = scriptRepository.read(scriptPath).orElse(null);
                if (script == null) {
                    LOG.error("Cron schedule script '{}' not found in repository!", scriptPath);
                } else {
                    if (ScriptJobUtils.checkScript(script, resourceResolver, executor, executionQueue)) {
                        ScriptJobUtils.queueScript(script, config.userImpersonationId(), executionQueue);
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
}