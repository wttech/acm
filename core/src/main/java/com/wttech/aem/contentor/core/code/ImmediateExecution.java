package com.wttech.aem.contentor.core.code;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

public class ImmediateExecution implements Execution {

    private final Executable executable;

    private final String id;

    private final ExecutionStatus status;

    private final Date startDate;

    private final Date endDate;

    private final String output;

    private final String error;

    public ImmediateExecution(Executable executable, String id, ExecutionStatus status, Date startedDate, String output, String error) {
        this.executable = executable;
        this.id = id;
        this.status = status;
        this.startDate = startedDate;
        this.endDate = new Date();
        this.output = output;
        this.error = error;
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
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public Date getEndDate() {
        return endDate;
    }

    @Override
    public long getDuration() {
        return endDate.getTime() - startDate.getTime();
    }

    @Override
    public String getOutput() {
        return output;
    }

    @Override
    public String getError() {
        return error;
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
