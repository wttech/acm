package dev.vml.es.acm.core.code;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.vml.es.acm.core.util.DateUtils;
import dev.vml.es.acm.core.util.NumberUtils;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.sling.event.jobs.Job;

public class QueuedExecutionSummary implements ExecutionSummary {

    @JsonIgnore
    private final transient Executor executor;

    @JsonIgnore
    private final transient Job job;

    public QueuedExecutionSummary(Executor executor, Job job) {
        this.executor = executor;
        this.job = job;
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
    public String getExecutableId() {
        return job.getProperty(ExecutionJob.EXECUTABLE_ID_PROP, String.class);
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
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", getId())
                .append("userId", getUserId())
                .append("executableId", getExecutableId())
                .append("status", getStatus())
                .append("duration", getDuration())
                .toString();
    }
}
