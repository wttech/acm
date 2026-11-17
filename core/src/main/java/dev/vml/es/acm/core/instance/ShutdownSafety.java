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
                    + " e.g. when a pod is replaced during a rolling deployment. Off by default.")
    public @interface Config {

        @AttributeDefinition(
                name = "Enabled",
                description = "Main switch. When off, ACM behaves as before and the options below have no effect.")
        boolean enabled() default false;

        @AttributeDefinition(
                name = "Abort On Shutdown",
                description = "When the execution queue is no longer available (the instance or ACM is stopping),"
                        + " end the running execution as ABORTED with the cause INSTANCE_SHUTDOWN instead of FAILED."
                        + " Scripts notice it on their next abort check.")
        boolean abortOnShutdown() default true;
    }

    private volatile Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
    }

    public boolean isAbortOnShutdown() {
        return config.enabled() && config.abortOnShutdown();
    }
}
