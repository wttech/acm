package com.wttech.aem.contentor.core.code;

import java.io.Serializable;
import java.util.Date;

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
