package com.wttech.aem.acm.core.code;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wttech.aem.acm.core.util.DateUtils;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.sling.event.jobs.Job;

public class QueuedExecution implements Execution {

    @JsonIgnore
    private final Executor executor;

    @JsonIgnore
    private final Job job;

    public QueuedExecution(Executor executor, Job job) {
        this.executor = executor;
        this.job = job;
    }

    protected Job getJob() {
        return job;
    }

    @Override
    public Executable getExecutable() {
        return Code.fromJob(job);
    }

    @Override
    public String getId() {
        return job.getId();
    }

    @Override
    public ExecutionStatus getStatus() {
        return ExecutionStatus.of(executor, job);
    }

    @Override
    public Date getStartDate() {
        return DateUtils.toDate(job.getProcessingStarted());
    }

    @Override
    public Date getEndDate() {
        return DateUtils.toDate(job.getFinishedDate());
    }

    @Override
    public long getDuration() {
        if (getStartDate() == null || getEndDate() == null) {
            return 0L;
        }
        return getEndDate().getTime() - getStartDate().getTime();
    }

    @Override
    public String getOutput() {
        return new ExecutionFileOutput(job.getId()).readString().orElse(null);
    }

    @Override
    public String getError() {
        return null;
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
