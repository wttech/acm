package com.vml.es.aem.acm.core.servlet;

import com.vml.es.aem.acm.core.code.Execution;
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
