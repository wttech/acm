package com.wttech.aem.contentor.core.code;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

// TODO implement read/write to JCR repository
public class HistoricalExecution implements Execution {

    private final Executable executable;

    private final String id;

    private final ExecutionStatus status;

    private final long duration;

    private final String error;

    public HistoricalExecution(Resource resource) {
        this.executable = null;
        this.id = null;
        this.status = null;
        this.duration = 0L;
        this.error = null;
    }

    public static HistoricalExecution read(Resource resource) {
        return new HistoricalExecution(resource);
    }

    public static void write(ResourceResolver resolver, String path, Execution execution) {
        // TODO write to repository
    }

    @Override
    public Executable getExecutable() {
        return executable;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ExecutionStatus getStatus() {
        return status;
    }

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public String getOutput() {
        return null; // TODO from resource
    }

    @Override
    public String getError() {
        return error; // TODO from resource
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("executable", getExecutable())
                .append("status", getStatus())
                .append("duration", getDuration())
                .toString();
    }
}
