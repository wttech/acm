package dev.vml.es.acm.core.instance;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = ShutdownSafety.class)
@Designate(ocd = ShutdownSafety.Config.class)
public class ShutdownSafety {

    @ObjectClassDefinition(
            name = "AEM Content Manager - Instance Shutdown Safety (experimental)",
            description = "Experimental. Controls how ACM behaves when an instance is shutting down,"
                    + " e.g. when a pod is replaced during a rolling deployment. Every option is off by default."
                    + " Turn options on one by one, first on a test environment.")
    public @interface Config {

        @AttributeDefinition(
                name = "Abort On Shutdown",
                description = "When on, a run that finds the execution queue unavailable (the instance or ACM is"
                        + " stopping) ends as ABORTED with the cause INSTANCE_SHUTDOWN instead of FAILED. Scripts"
                        + " notice it on their next abort check.")
        boolean abortOnShutdown() default false;

        @AttributeDefinition(
                name = "Keep Cron Until Healthy",
                description = "When on, a boot removes the cron schedules only after the instance is healthy, right"
                        + " before it schedules them again. If the instance never gets healthy, the schedules stay.")
        boolean keepCronUntilHealthy() default false;

        @AttributeDefinition(
                name = "Boot On Instance Left",
                description =
                        "When on, the leader boots automatic scripts again when another instance leaves the"
                                + " cluster, so a script stopped by a shutdown can run again on an instance that is still"
                                + " running. Every instance logs which instances left. Turn on 'Keep Cron Until Healthy'"
                                + " too, so these extra boots do not remove the cron schedules while the instance is unhealthy.")
        boolean bootOnInstanceLeft() default false;

        @AttributeDefinition(
                name = "Rerun Killed Execution",
                description = "When on, the execution lock of an automatic script stores the ID of its run and a time"
                        + " the run refreshes while it works. When an instance is killed and Sling delivers the"
                        + " unfinished job again (after a restart, or on another instance of the cluster), the script"
                        + " runs again once under its own lock instead of waiting for the lock timeout. A run that"
                        + " cannot refresh its lock for too long, or whose lock is held by another run, stops at its"
                        + " next abort check and ends as aborted. The times come from the alive timeout below."
                        + " Other scripts keep the usual lock.")
        boolean rerunKilledExecution() default false;

        @AttributeDefinition(
                name = "Rerun Killed Execution Alive Timeout",
                description = "Time in milliseconds without a refresh after which a job delivered again treats the"
                        + " earlier run of an automatic script as killed and runs it again. The run refreshes its lock"
                        + " every 1/6 of this time. After 2/3 of it without a successful refresh, the script stops at"
                        + " its next abort check and ends as aborted. The last 1/3 must be longer than the clock"
                        + " difference between instances. A lower value runs again sooner after a kill, but writes"
                        + " more often and stops a healthy script sooner when the repository is slow. Minimum 6000."
                        + " A lower value is raised to the minimum. The value is stored on each lock.")
        long rerunKilledExecutionAliveTimeout() default 30_000L;
    }

    private static final Logger LOG = LoggerFactory.getLogger(ShutdownSafety.class);

    public static final long MIN_RERUN_KILLED_EXECUTION_ALIVE_TIMEOUT = 6_000L;

    private volatile Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        if (config.rerunKilledExecutionAliveTimeout() < MIN_RERUN_KILLED_EXECUTION_ALIVE_TIMEOUT) {
            LOG.warn(
                    "Rerun killed execution alive timeout {} ms is too short. Using the minimum of {} ms.",
                    config.rerunKilledExecutionAliveTimeout(),
                    MIN_RERUN_KILLED_EXECUTION_ALIVE_TIMEOUT);
        }
    }

    public boolean isAbortOnShutdown() {
        return config.abortOnShutdown();
    }

    public boolean isKeepCronUntilHealthy() {
        return config.keepCronUntilHealthy();
    }

    public boolean isBootOnInstanceLeft() {
        return config.bootOnInstanceLeft();
    }

    public boolean isRerunKilledExecution() {
        return config.rerunKilledExecution();
    }

    public long getRerunKilledExecutionAliveTimeout() {
        return Math.max(MIN_RERUN_KILLED_EXECUTION_ALIVE_TIMEOUT, config.rerunKilledExecutionAliveTimeout());
    }
}
