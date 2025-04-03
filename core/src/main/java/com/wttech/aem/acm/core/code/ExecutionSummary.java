package com.wttech.aem.acm.core.code;

import java.io.Serializable;
import java.util.Date;

/**
 * Lightweight representation of an execution.
 * Skips executable arguments, error and output (heavy data).
 */
public interface ExecutionSummary extends Serializable {

    String getId();

    String getExecutableId();

    ExecutionStatus getStatus();

    Date getStartDate();

    Date getEndDate();

    long getDuration();
}
