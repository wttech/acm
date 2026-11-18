package dev.vml.es.acm.core.instance;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(immediate = true, service = ShutdownSafety.class)
@Designate(ocd = ShutdownSafety.Config.class)
public class ShutdownSafety {

    @ObjectClassDefinition(
            name = "AEM Content Manager - Instance Shutdown Safety",
            description = "Controls how ACM behaves when an instance is shutting down,"
                    + " e.g. when a pod is replaced during a rolling deployment. Every option is off by default.")
    public @interface Config {

        @AttributeDefinition(
                name = "Abort On Shutdown",
                description = "When on, a run that finds the execution queue unavailable (the instance or ACM is"
                        + " stopping) ends as ABORTED with the cause INSTANCE_SHUTDOWN instead of FAILED; scripts"
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
    }

    private volatile Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
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
}
