package com.wttech.aem.contentor.core.code;

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

    private final Job job;

    public QueuedExecution(Job job) {
        this.job = job;
    }

    static String composeJobResultMessage(Execution execution) throws ContentorException {
        try {
            Map<String, Object> props = new HashMap<>();
            props.put("status", execution.getStatus().name());
            props.put("startDate", DateUtils.toString(execution.getStartDate()));
            props.put("endDate", DateUtils.toString(execution.getEndDate()));
            return JsonUtils.mapToString(props);
        } catch (IOException e) {
            throw new ContentorException("Failed to compose job result message!", e);
        }
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
        return Optional.ofNullable(getJobResultMessageProps().get("status"))
                .flatMap(s -> ExecutionStatus.of((String) s))
                .orElseGet(() -> ExecutionStatus.of(job));
    }

    @Override
    public Date getStartDate() {
        return Optional.ofNullable(getJobResultMessageProps().get("startDate"))
                .map(d -> DateUtils.fromString((String) d))
                .orElseGet(() -> DateUtils.toDate(job.getCreated()));
    }

    @Override
    public Date getEndDate() {
        return Optional.ofNullable(getJobResultMessageProps().get("endDate"))
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

    private Map<String, Object> getJobResultMessageProps() throws ContentorException {
        try {
            return JsonUtils.stringToMap(job.getResultMessage());
        } catch (IOException e) {
            throw new ContentorException("Failed to parse job result message properties!", e);
        }
    }

    @Override
    public String getError() {
        return ExecutionQueue.readFile(job.getId(), ExecutionQueue.FileType.ERROR)
                .orElse(null);
    }

    @Override
    public String getOutput() {
        return ExecutionQueue.readFile(job.getId(), ExecutionQueue.FileType.OUTPUT)
                .orElse(null);
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
