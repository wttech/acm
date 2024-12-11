package com.wttech.aem.contentor.core.code;

import java.io.Serializable;

public interface Execution extends Serializable {

    Executable getExecutable();

    String getId();

    ExecutionStatus getStatus();

    long getDuration();

    String getError();

    String getOutput();
}
