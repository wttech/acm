package dev.vml.es.acm.core.servlet.output;

import dev.vml.es.acm.core.code.Execution;
import java.io.Serializable;
import java.util.List;

public class QueueOutput implements Serializable {

    private List<Execution> executions;

    public QueueOutput(List<Execution> executions) {
        this.executions = executions;
    }

    public List<Execution> getExecutions() {
        return executions;
    }
}
