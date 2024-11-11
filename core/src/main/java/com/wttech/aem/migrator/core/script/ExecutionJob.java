package com.wttech.aem.migrator.core.script;

import java.io.Serializable;

public class ExecutionJob implements Serializable {

    private final String id;

    private final String state;

    public ExecutionJob(String id, String state) {
        this.id = id;
        this.state = state;
    }

    public String getId() {
        return id;
    }

    public String getState() {
        return state;
    }
}
