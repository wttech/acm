package com.vml.es.aem.acm.core.gui;

import java.io.Serializable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(service = SpaSettings.class, immediate = true)
@Designate(ocd = SpaSettings.Config.class)
public class SpaSettings implements Serializable {

    private long appStateInterval;

    private long executionPollInterval;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.appStateInterval = config.appStateInterval();
        this.executionPollInterval = config.executionPollInterval();
    }

    public long getAppStateInterval() {
        return appStateInterval;
    }

    public long getExecutionPollInterval() {
        return executionPollInterval;
    }

    @ObjectClassDefinition(name = "AEM Content Manager - SPA Settings")
    public @interface Config {

        @AttributeDefinition(
                name = "Application State Interval",
                description = "Interval in milliseconds to check application state.")
        long appStateInterval() default 3000;

        @AttributeDefinition(
                name = "Execution Poll Interval",
                description = "Interval in milliseconds to poll execution status.")
        long executionPollInterval() default 1000;
    }
}
