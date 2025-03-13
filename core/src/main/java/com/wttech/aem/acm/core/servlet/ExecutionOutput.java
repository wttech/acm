package com.wttech.aem.acm.core.servlet;

import com.wttech.aem.acm.core.code.Execution;
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
