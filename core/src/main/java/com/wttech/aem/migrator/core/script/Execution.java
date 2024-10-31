package com.wttech.aem.migrator.core.script;

import java.io.Serializable;
import java.time.Duration;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Execution implements Serializable {

    private final Executable executable;

    private final Status status;

    private final Duration duration;

    public Execution(Executable executable, Status status, Duration duration) {
        this.executable = executable;
        this.status = status;
        this.duration = duration;
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
                .append("status", status)
                .append("duration", duration)
                .toString();
    }

    public enum Status {
        SUCCESS,
        FAILURE
    }
}
