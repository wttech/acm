package dev.vml.es.acm.core.servlet;

import java.io.Serializable;
import java.util.List;

public class ExecutionOutput implements Serializable {

    @SuppressWarnings("java:S1948")
    private List<?> list;

    public ExecutionOutput(List<?> executions) {
        this.list = executions;
    }

    public List<?> getList() {
        return list;
    }
}
