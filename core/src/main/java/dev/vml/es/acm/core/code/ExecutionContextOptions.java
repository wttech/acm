package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.util.JsonUtils;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.sling.event.jobs.Job;

/**
 * Determines creation of execution context in an asynchronous job.
 */
public class ExecutionContextOptions implements Serializable {

    private final ExecutionMode executionMode;

    private final String userId;

    private final InputValues inputs;

    public ExecutionContextOptions(ExecutionMode executionMode, String userId, InputValues inputs) {
        this.executionMode = executionMode;
        this.userId = Objects.requireNonNull(userId);
        this.inputs = inputs;
    }

    public static Map<String, Object> toJobProps(ExecutionContextOptions options) throws AcmException {
        try {
            Map<String, Object> props = new HashMap<>();
            props.put(
                    ExecutionJob.EXECUTION_MODE_PROP, options.getExecutionMode().name());
            props.put(ExecutionJob.USER_ID_PROP, options.getUserId());
            props.put(ExecutionJob.INPUTS_PROP, JsonUtils.writeToString(options.getInputs()));
            return props;
        } catch (IOException e) {
            throw new AcmException("Cannot serialize input values to JSON!", e);
        }
    }

    public static ExecutionContextOptions fromJob(Job job) {
        try {
            return new ExecutionContextOptions(
                    ExecutionMode.valueOf(job.getProperty(ExecutionJob.EXECUTION_MODE_PROP, String.class)),
                    job.getProperty(ExecutionJob.USER_ID_PROP, String.class),
                    JsonUtils.readFromString(
                            job.getProperty(ExecutionJob.INPUTS_PROP, String.class), InputValues.class));
        } catch (IOException e) {
            throw new AcmException("Cannot deserialize execution context values from JSON!", e);
        }
    }

    public String getUserId() {
        return userId;
    }

    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public InputValues getInputs() {
        return inputs;
    }
}
