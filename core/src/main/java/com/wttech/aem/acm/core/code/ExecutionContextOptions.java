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

    public static final String USER_ID_PROP = "userId";

    public static final String EXECUTION_MODE_PROP = "executionMode";

    private final ExecutionMode executionMode;

    private final String userId;

    public ExecutionContextOptions(ExecutionMode executionMode, String userId) {
        this.executionMode = executionMode;
        this.userId = userId;
    }

    public static Map<String, Object> toJobProps(ExecutionContextOptions options) throws AcmException {
        Map<String, Object> props = new HashMap<>();
        props.put(EXECUTION_MODE_PROP, options.getExecutionMode().name());
        props.put(USER_ID_PROP, options.getUserId());
        return props;
    }

    public static ExecutionContextOptions fromJob(Job job) {
        return new ExecutionContextOptions(
                ExecutionMode.valueOf(job.getProperty(EXECUTION_MODE_PROP, String.class)),
                job.getProperty(USER_ID_PROP, String.class));
    }

    public String getUserId() {
        return userId;
    }

    public ExecutionMode getExecutionMode() {
        return executionMode;
    }
}
