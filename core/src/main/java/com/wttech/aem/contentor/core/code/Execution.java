package com.wttech.aem.contentor.core.code;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

public class Execution implements Serializable {

    private final Executable executable;

    private final String id;

    private final ExecutionStatus status;

    private final long duration;

    private final String output;

    private final String error;

    public Execution(
            Executable executable, String id, ExecutionStatus status, long duration, String output, String error) {
        this.executable = executable;
        this.id = id;
        this.status = status;
        this.duration = duration;
        this.output = output;
        this.error = error;
    }

    public Executable getExecutable() {
        return executable;
    }

    public String getId() {
        return id;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public long getDuration() {
        return duration;
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }

    public String toString() {
        return new ToStringBuilder(this)
                .append("executable", executable)
                .append("status", status)
                .append("duration", duration)
                .append("output", StringUtils.abbreviate(output, 1024))
                .append("error", StringUtils.abbreviate(error, 1024))
                .toString();
    }
}
