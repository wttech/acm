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

    private int executionCodeOutputChunkSize;

    private int executionFileOutputChunkSize;

    private long scriptStatsLimit;

    private boolean scriptManagementEnabled;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.appStateInterval = config.appStateInterval();
        this.executionPollInterval = config.executionPollInterval();
        this.executionCodeOutputChunkSize = config.executionCodeOutputChunkSize();
        this.executionFileOutputChunkSize = config.executionFileOutputChunkSize();
        this.scriptStatsLimit = config.scriptStatsLimit();
        this.scriptManagementEnabled = config.scriptManagementEnabled();
    }

    public long getAppStateInterval() {
        return appStateInterval;
    }

    public long getExecutionPollInterval() {
        return executionPollInterval;
    }

    public int getExecutionCodeOutputChunkSize() {
        return executionCodeOutputChunkSize;
    }

    public int getExecutionFileOutputChunkSize() {
        return executionFileOutputChunkSize;
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

        @AttributeDefinition(name = "Execution Code Output Chunk Size", description = "In bytes. Default is 2 MB.")
        int executionCodeOutputChunkSize() default 2 * 1024 * 1024;

        @AttributeDefinition(name = "Execution File Output Chunk Size", description = "In bytes. Default is 10 MB.")
        int executionFileOutputChunkSize() default 10 * 1024 * 1024;

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
