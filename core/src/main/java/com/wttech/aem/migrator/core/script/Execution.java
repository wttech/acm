package com.wttech.aem.migrator.core.script;

import java.io.Serializable;
import java.time.Duration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Execution implements Serializable {

    private final Executable executable;

    private final Status status;

    private final Duration duration;

    private final String output;

    private final Exception exception;

    public Execution(Executable executable, Status status, Duration duration, String output, Exception exception) {
        this.executable = executable;
        this.status = status;
        this.duration = duration;
        this.output = output;
        this.exception = exception;
    }

    public Executable getExecutable() {
        return executable;
    }

    public Status getStatus() {
        return status;
    }

    public Duration getDuration() {
        return duration;
    }

    public String toString() {
        return new ToStringBuilder(this)
                .append("executable", executable)
                .append("status", status)
                .append("duration", duration)
                .append("output", StringUtils.abbreviate(output, 1024))
                .append("exception", StringUtils.abbreviate(exception.getMessage(), 1024))
                .toString();
    }

    public enum Status {
        SUCCESS,
        FAILURE
    }
}
