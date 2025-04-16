package com.vml.es.aem.acm.core.servlet;

import java.io.Serializable;
import java.util.List;

public class ExecutionOutput implements Serializable {

    public List<?> list;

    public ExecutionOutput(List<?> executions) {
        this.list = executions;
    }

    public List<?> getList() {
        return list;
    }
}
