package com.wttech.aem.migrator.core.script;

import java.io.Serializable;

public class ExecutionJob implements Serializable {

    private final String id;

    private final String state;

    private final String output;

    public ExecutionJob(String id, String state, String output) {
        this.id = id;
        this.state = state;
        this.output = output;
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
}
