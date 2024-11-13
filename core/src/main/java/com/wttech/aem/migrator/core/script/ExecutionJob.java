package com.wttech.aem.migrator.core.script;

import com.wttech.aem.migrator.core.util.ExceptionUtils;
import java.io.Serializable;

public class ExecutionJob implements Serializable {

    private final String id;

    private final String state;

    private final String output;

    private final String error;

    public ExecutionJob(String id, String state, String output, Throwable throwable) {
        this.id = id;
        this.state = state;
        this.output = output;
        this.error = ExceptionUtils.toString(throwable);
    }

    public String getId() {
        return id;
    }

    public String getState() {
        return state;
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }
}
