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
                description =
                        "When on, a run that finds the execution queue unavailable (the instance or ACM is"
                                + " stopping) ends as ABORTED with the cause INSTANCE_SHUTDOWN instead of FAILED; scripts"
                                + " notice it on their next abort check. A boot removes the cron schedules only after the"
                                + " instance is healthy, right before it schedules them again. When off, ACM behaves as before.")
        boolean enabled() default false;
    }

    private volatile Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return config.enabled();
    }
}
