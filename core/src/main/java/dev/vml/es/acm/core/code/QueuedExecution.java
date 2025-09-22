package dev.vml.es.acm.core.code;

import java.util.Date;
import java.util.Optional;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.sling.event.jobs.Job;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.vml.es.acm.core.util.DateUtils;
import dev.vml.es.acm.core.util.NumberUtils;

public class QueuedExecution implements Execution {

    @JsonIgnore
    private final transient Executor executor;

    @JsonIgnore
    private final transient Job job;

    @JsonIgnore
    private final transient CodeOutputFile outputFile;

    public QueuedExecution(Executor executor, Job job, FileManager fileManager) {
        this.executor = executor;
        this.job = job;
        this.outputFile = new CodeOutputFile(fileManager, job.getId());
    }

    protected Job getJob() {
        return job;
    }

    @Override
    public String getId() {
        return job.getId();
    }

    @Override
    public String getUserId() {
        return job.getProperty(ExecutionJob.USER_ID_PROP, String.class);
    }

    @Override
    public ExecutionStatus getStatus() {
        return ExecutionStatus.of(job, executor);
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
        return NumberUtils.durationBetween(getStartDate(), getEndDate());
    }

    @Override
    public String getOutput() {
        return outputFile.readString().orElse(null);
    }

    @Override
    public String getInstance() {
        return null; // not needed at the moment
    }

    @Override
    public String getError() {
        return Optional.ofNullable(job.getResultMessage())
                .map(QueuedMessage::fromJson)
                .map(QueuedMessage::getError)
                .orElse(null);
    }

    @Override
    public Executable getExecutable() {
        return Code.fromJob(job);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", getId())
                .append("userId", getUserId())
                .append("executable", getExecutable())
                .append("status", getStatus())
                .append("duration", getDuration())
                .toString();
    }
}
