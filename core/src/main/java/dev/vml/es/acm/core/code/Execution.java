package dev.vml.es.acm.core.code;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.Date;

/**
 * Full representation of an execution.
 * Contains heavy data (executable inputs, outputs, error and output, instance state).
 */
public interface Execution extends Serializable {

    String getId();

    String getUserId();

    ExecutionStatus getStatus();

    Date getStartDate();

    Date getEndDate();

    long getDuration();

    String getOutput();

    String getError();

    InputValues getInputs();

    OutputValues getOutputs();

    String getInstance();

    Executable getExecutable();

    /** Set when the system aborted the run (e.g. instance shutdown); null otherwise, also for a user abort. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    default AbortCause getAbortCause() {
        return null;
    }
}
