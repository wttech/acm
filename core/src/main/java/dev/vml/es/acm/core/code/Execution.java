package dev.vml.es.acm.core.code;

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

    Outputs getOutputs();

    String getInstance();

    Executable getExecutable();
}
