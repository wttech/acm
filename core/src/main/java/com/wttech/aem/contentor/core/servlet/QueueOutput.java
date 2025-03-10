package com.wttech.aem.contentor.core.servlet;

import com.wttech.aem.contentor.core.code.Execution;
import java.io.Serializable;
import java.util.List;

public class QueueOutput implements Serializable {

    public List<Execution> executions;

    public QueueOutput(List<Execution> executions) {
        this.executions = executions;
    }

    public List<Execution> getExecutions() {
        return executions;
    }
}
