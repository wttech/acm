package com.wttech.aem.contentor.core.script;

import com.wttech.aem.contentor.core.code.ExecutionQueue;
import com.wttech.aem.contentor.core.code.Executor;
import com.wttech.aem.contentor.core.instance.HealthChecker;
import com.wttech.aem.contentor.core.instance.HealthStatus;
import com.wttech.aem.contentor.core.util.ResourceUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        immediate = true,
        service = {Runnable.class})
@Designate(ocd = ScriptExecutor.Config.class)
public class ScriptExecutor implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ScriptExecutor.class);

    @ObjectClassDefinition(name = "AEM Contentor - Script Executor")
    public @interface Config {

        @AttributeDefinition(name = "Enabled", description = "Allows to temporarily disable the script executor")
        boolean enabled() default true;

        @AttributeDefinition(
                name = "Debug mode",
                description =
                        "Enables debug mode for troubleshooting. Changed behaviors include: start saving skipped executions in history.")
        boolean debug() default false;

        @AttributeDefinition(
                name = "Scheduler expression",
                description =
                        "How often the scripts should be executed. Default is every 30 seconds (0/30 * * * * ?). Quartz cron expression format.",
                defaultValue = "0/30 * * * * ?")
        String scheduler_expression() default "0/30 * * * * ?";
    }

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Executor executor;

    @Reference
    private HealthChecker healthChecker;

    @Reference
    private ExecutionQueue queue;

    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
    }

    @Override
    public void run() {
        if (!config.enabled()) {
            LOG.debug("Script executor is disabled");
            return;
        }
        HealthStatus healthStatus = healthChecker.checkStatus();
        if (!healthStatus.isHealthy()) {
            LOG.error("Script executor is paused due to health status: {}", healthStatus);
            return;
        }

        try (ResourceResolver resourceResolver = ResourceUtils.serviceResolver(resourceResolverFactory)) {
            ScriptRepository scriptRepository = new ScriptRepository(resourceResolver);
            scriptRepository.findAll(ScriptType.ENABLED).forEach(script -> {
                queue.submit(script);
            });
        } catch (Exception e) {
            LOG.error("Failed to access repository while submitting enabled scripts to execution queue", e);
        }
    }
}
