package dev.vml.es.acm.core.script;

import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.code.*;
import dev.vml.es.acm.core.instance.HealthChecker;
import dev.vml.es.acm.core.instance.HealthStatus;
import dev.vml.es.acm.core.util.ResourceUtils;
import dev.vml.es.acm.core.util.quartz.CronExpression;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
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
        service = {Runnable.class, ScriptScheduler.class},
        property = {Scheduler.PROPERTY_SCHEDULER_RUN_ON + "=" + Scheduler.VALUE_RUN_ON_LEADER})
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
                        "How often the scripts should be executed. Default is every minute (0 * * * * ?). Quartz cron expression format.")
        String scheduler_expression() default "0 * * * * ?";

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

    private long intervalMillis;

    private final AtomicLong runCount = new AtomicLong(0);

    private final AtomicBoolean boot = new AtomicBoolean(true);

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        this.intervalMillis = calculateInterval();
    }

    private long calculateInterval() {
        try {
            CronExpression expression = new CronExpression(this.config.scheduler_expression());
            Date now = new Date();
            Date nextRun = expression.getNextValidTimeAfter(now);
            Date secondRun = expression.getNextValidTimeAfter(nextRun);
            return secondRun.getTime() - nextRun.getTime();
        } catch (ParseException e) {
            throw new AcmException("Script scheduler interval cannot be parsed!", e);
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
        List<Execution> bootScripts = findQueuedBootScripts().collect(Collectors.toList());
        if (!bootScripts.isEmpty()) {
            LOG.info("Script scheduler is paused due to queued boot scripts ({}): {}", bootScripts.size(), StringUtils.join(bootScripts, ", "));
            return;
        }
        queueScripts();
    }

    private void queueScripts() {
        String userId =
                StringUtils.defaultIfBlank(config.userImpersonationId(), ResourceUtils.Subservice.CONTENT.userId);
        ExecutionContextOptions contextOptions = new ExecutionContextOptions(ExecutionMode.RUN, userId);
        try (ResourceResolver resourceResolver =
                ResourceUtils.contentResolver(resourceResolverFactory, contextOptions.getUserId())) {
            if (boot.compareAndSet(true, false)) {
                queueScripts(ScriptType.BOOT, resourceResolver, contextOptions);
            } else {
                queueScripts(ScriptType.SCHEDULE, resourceResolver, contextOptions);
            }
            runCount.incrementAndGet();
        } catch (Exception e) {
            LOG.error("Cannot access repository while scheduling enabled scripts to execution queue!", e);
        }
    }

    private void queueScripts(ScriptType type, ResourceResolver resourceResolver, ExecutionContextOptions contextOptions) {
        ScriptRepository scriptRepository = new ScriptRepository(resourceResolver);
        scriptRepository.findAll(type).forEach(script -> {
            if (checkScript(script, resourceResolver)) {
                queueScript(script, contextOptions);
            }
        });
    }

    private boolean checkScript(Script script, ResourceResolver resourceResolver) {
        try (ExecutionContext context =
                executor.createContext(ExecutionId.generate(), ExecutionMode.CHECK, script, resourceResolver)) {
            Execution execution = executor.execute(context);
            LOG.debug("Script checked '{}'", execution);
            return execution.getStatus() != ExecutionStatus.SKIPPED;
        } catch (Exception e) {
            LOG.error("Cannot check script '{}' while scheduling to execution queue!", script.getId(), e);
            return false;
        }
    }

    private void queueScript(Script script, ExecutionContextOptions contextOptions) {
        try {
            queue.submit(script, contextOptions);
        } catch (Exception e) {
            LOG.error("Cannot submit script '{}' to execution queue!", script.getId(), e);
        }
    }

    private Stream<Execution> findQueuedBootScripts() {
        return queue.findAll().filter(execution -> {
            ScriptType type = ScriptType.byPath(execution.getExecutable().getId()).orElse(null);
            return ScriptType.BOOT.equals(type);
        });
    }

    public long getIntervalMillis() {
        return this.intervalMillis;
    }

    public long getRunCount() {
        return this.runCount.get();
    }
}
