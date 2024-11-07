package com.wttech.aem.migrator.core.script;

import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class Execution implements Serializable {

    private final Executable executable;

    private final Status status;

    private final long duration;

    private final String output;

    private final String error;

    public Execution(Executable executable, Status status, long duration, String output, Throwable throwable) {
        this.executable = executable;
        this.status = status;
        this.duration = duration;
        this.output = output;
        this.error = composeError(throwable);
    }

    private String composeError(Throwable cause) {
        var builder = new StringBuilder();
        if (cause != null) {
            var rootCause = ExceptionUtils.getRootCause(cause);
            if (rootCause != cause) {
                builder.append(rootCause.getMessage()).append("\n");
                builder.append(ExceptionUtils.getStackTrace(rootCause));
                builder.append("\n\n");
            }

            builder.append(cause.getMessage()).append("\n");
            builder.append(ExceptionUtils.getStackTrace(cause));
        }
        return builder.toString();
    }

    public Executable getExecutable() {
        return executable;
    }

    public Status getStatus() {
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

    public enum Status {
        SUCCESS,
        FAILURE
    }
}
