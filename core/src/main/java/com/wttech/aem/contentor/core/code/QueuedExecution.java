package com.wttech.aem.contentor.core.code;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wttech.aem.contentor.core.ContentorException;
import com.wttech.aem.contentor.core.util.DateUtils;
import com.wttech.aem.contentor.core.util.JsonUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.sling.event.jobs.Job;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class QueuedExecution implements Execution {

    @JsonIgnore
    private final Job job;

    public QueuedExecution(Job job) {
        this.job = job;
    }

    static String jobResultMessage(Execution execution) throws ContentorException {
        return jobResultMessage(execution.getStatus(), execution.getStartDate(), execution.getEndDate(), execution.getError());
    }

    static String jobResultMessage(ExecutionStatus status) throws ContentorException {
        return jobResultMessage(status, null, null, null);
    }

    static String jobResultMessage(ExecutionStatus status, Date startDate, Date endDate, String error) throws ContentorException {
        try {
            Map<String, Object> props = new HashMap<>();
            props.put("status", status.name());
            props.put("startDate", DateUtils.toString(startDate));
            props.put("endDate", DateUtils.toString(endDate));
            props.put("error", error);
            return JsonUtils.mapToString(props);
        } catch (IOException e) {
            throw new ContentorException("Failed to compose job result message!", e);
        }
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
        return Optional.ofNullable(getJobResultProps().get("status"))
                .flatMap(s -> ExecutionStatus.of((String) s))
                .orElseGet(() -> ExecutionStatus.of(job));
    }

    @Override
    public Date getStartDate() {
        return Optional.ofNullable(getJobResultProps().get("startDate"))
                .map(d -> DateUtils.fromString((String) d))
                .orElseGet(() -> DateUtils.toDate(job.getProcessingStarted()));
    }

    @Override
    public Date getEndDate() {
        return Optional.ofNullable(getJobResultProps().get("endDate"))
                .map(d -> DateUtils.fromString((String) d))
                .orElseGet(() -> DateUtils.toDate(job.getFinishedDate()));
    }

    @Override
    public long getDuration() {
        if (getStartDate() == null || getEndDate() == null) {
            return 0L;
        }
        return getEndDate().getTime() - getStartDate().getTime();
    }

    private Map<String, Object> getJobResultProps() throws ContentorException {
        try {
            return JsonUtils.stringToMap(job.getResultMessage());
        } catch (IOException e) {
            throw new ContentorException("Failed to parse job result message properties!", e);
        }
    }

    @Override
    public String getError() {
        return (String) getJobResultProps().get("error");
    }

    @Override
    public String getOutput() {
        return ExecutionFile.read(job.getId(), ExecutionFile.OUTPUT).orElse(null);
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
