package com.wttech.aem.acm.core.code;

import java.io.Serializable;
import java.util.Date;

/**
 * Full representation of an execution.
 * Contains heavy data (executable arguments, error and output).
 */
public interface Execution extends Serializable {

    Executable getExecutable();

    String getId();

    ExecutionStatus getStatus();

    Date getStartDate();

    Date getEndDate();

    long getDuration();

    String getError();

    String getOutput();
}
