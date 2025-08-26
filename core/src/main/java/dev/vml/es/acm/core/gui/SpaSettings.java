package dev.vml.es.acm.core.gui;

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

    private long scriptStatsLimit;

    private boolean scriptManagementEnabled;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.appStateInterval = config.appStateInterval();
        this.executionPollInterval = config.executionPollInterval();
        this.scriptStatsLimit = config.scriptStatsLimit();
        this.scriptManagementEnabled = config.scriptManagementEnabled();
    }

    public long getAppStateInterval() {
        return appStateInterval;
    }

    public long getExecutionPollInterval() {
        return executionPollInterval;
    }

    public long getScriptStatsLimit() {
        return scriptStatsLimit;
    }

    public boolean isScriptManagementEnabled() {
        return scriptManagementEnabled;
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
        long executionPollInterval() default 1400;

        @AttributeDefinition(
                name = "Script Stats Limit",
                description =
                        "Limit for the number of historical executions to be considered to calculate the average duration.")
        long scriptStatsLimit() default 10;

        @AttributeDefinition(
                name = "Script Management Enabled",
                description = "Enable or disable script management features (delete, save, sync, etc).")
        boolean scriptManagementEnabled() default true;
    }
}
