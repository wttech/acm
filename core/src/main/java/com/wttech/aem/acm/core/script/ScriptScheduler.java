package com.wttech.aem.acm.core.script;

import com.wttech.aem.acm.core.code.ExecutionContextOptions;
import com.wttech.aem.acm.core.code.ExecutionMode;
import com.wttech.aem.acm.core.code.ExecutionQueue;
import com.wttech.aem.acm.core.code.Executor;
import com.wttech.aem.acm.core.instance.HealthChecker;
import com.wttech.aem.acm.core.instance.HealthStatus;
import com.wttech.aem.acm.core.util.ResourceUtils;
import com.wttech.aem.acm.core.util.quartz.CronExpression;
import java.text.ParseException;
import java.util.Date;
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
        service = {Runnable.class, ScriptScheduler.class})
@Designate(ocd = ScriptScheduler.Config.class)
public class ScriptScheduler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ScriptScheduler.class);

    @ObjectClassDefinition(name = "AEM Content Manager - Script Scheduler")
    public @interface Config {

        @AttributeDefinition(name = "Enabled", description = "Allows to temporarily disable the script executor")
        boolean enabled() default true;

        @AttributeDefinition(
                name = "Scheduler Expression",
                description =
                        "How often the scripts should be executed. Default is every 30 seconds (0/30 * * * * ?). Quartz cron expression format.",
                defaultValue = "0/30 * * * * ?")
        String scheduler_expression() default "0/30 * * * * ?";

        @AttributeDefinition(
                name = "User Impersonation ID",
                description =
                        "Controls who accesses the repository when scripts are automatically executed. If blank, the service user is used.")
        String userImpersonationId();
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

    /** This function returns time between next 2 runs of provided cron job. If cron expression is invalid returned value will be -1. */
    public long getIntervalBetweenRuns() {
        try {
            CronExpression expression = new CronExpression(this.config.scheduler_expression());
            Date now = new Date();
            Date nextRun = expression.getNextValidTimeAfter(now);
            if (nextRun == null) {
                return -1;
            }
            Date secondRun = expression.getNextValidTimeAfter(nextRun);
            if (secondRun == null) {
                return -1;
            }
            return secondRun.getTime() - nextRun.getTime();
        } catch (ParseException e) {
            return -1;
        }
    }

    @Override
    public void run() {
        if (!config.enabled()) {
            LOG.debug("Script scheduler is disabled");
            return;
        }
        HealthStatus healthStatus = healthChecker.checkStatus();
        if (!healthStatus.isHealthy()) {
            LOG.warn("Script scheduler is paused due to health status: {}", healthStatus);
            return;
        }

        ExecutionContextOptions contextOptions =
                new ExecutionContextOptions(ExecutionMode.RUN, config.userImpersonationId());
        try (ResourceResolver resourceResolver =
                ResourceUtils.serviceResolver(resourceResolverFactory, contextOptions.getUserId())) {
            ScriptRepository scriptRepository = new ScriptRepository(resourceResolver);

            scriptRepository.clean();

            scriptRepository.findAll(ScriptType.ENABLED).forEach(script -> {
                queue.submit(contextOptions, script);
            });
        } catch (Exception e) {
            LOG.error("Failed to access repository while scheduling enabled scripts to execution queue", e);
        }
    }
}
