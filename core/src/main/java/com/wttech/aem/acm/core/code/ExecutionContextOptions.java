package com.wttech.aem.acm.core.code;

import com.wttech.aem.acm.core.AcmException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import org.apache.sling.event.jobs.Job;

/**
 * Determines creation of execution context in asynchronous job.
 */
public class ExecutionContextOptions implements Serializable {

    private String userId;

    public ExecutionContextOptions(String userId) {
        this.userId = userId;
    }

    public static Map<String, Object> toJobProps(ExecutionContextOptions options) throws AcmException {
        return Collections.singletonMap("userId", options.getUserId());
    }

    public static ExecutionContextOptions fromJob(Job job) {
        return new ExecutionContextOptions(job.getProperty("userId", String.class));
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
