package com.wttech.aem.contentor.core.code;

import com.wttech.aem.contentor.core.ContentorException;
import com.wttech.aem.contentor.core.util.JsonUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.sling.event.jobs.Job;

import java.io.IOException;
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
            props.put("duration", String.valueOf(execution.getDuration())); // JSON uses integers (avoid long -> int)
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
    public long getDuration() {
        return Optional.ofNullable(getJobResultMessageProps().get("duration"))
                .map(d -> Long.parseLong((String) d))
                .orElse(0L);
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
