package com.wttech.aem.acm.core.code;

import com.wttech.aem.acm.core.AcmException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.sling.event.jobs.Job;

/**
 * Determines creation of execution context in asynchronous job.
 */
public class ExecutionContextOptions implements Serializable {

    private final ExecutionMode executionMode;

    private final String userId;

    public ExecutionContextOptions(ExecutionMode executionMode, String userId) {
        this.executionMode = executionMode;
        this.userId = userId;
    }

    public static Map<String, Object> toJobProps(ExecutionContextOptions options) throws AcmException {
        Map<String, Object> props = new HashMap<>();
        props.put("executionMode", options.getExecutionMode().name());
        props.put("userId", options.getUserId());
        return props;
    }

    public static ExecutionContextOptions fromJob(Job job) {
        return new ExecutionContextOptions(
                ExecutionMode.valueOf(job.getProperty("executionMode", String.class)),
                job.getProperty("userId", String.class));
    }

    public String getUserId() {
        return userId;
    }

    public ExecutionMode getExecutionMode() {
        return executionMode;
    }
}
