package com.wttech.aem.contentor.core.servlet;

import com.wttech.aem.contentor.core.code.Execution;

import java.io.Serializable;
import java.util.List;

public class ExecutionOutput implements Serializable {

    public List<Execution> list;

    public ExecutionOutput(List<Execution> executions) {
        this.list = executions;
    }

    public List<Execution> getList() {
        return list;
    }
}
