package com.vml.es.aem.acm.core.code;

import java.io.Serializable;

public class Description implements Serializable {

    private final Execution execution;

    private final Arguments arguments;

    public Description(Execution execution, Arguments arguments) {
        this.execution = execution;
        this.arguments = arguments;
    }

    public Execution getExecution() {
        return execution;
    }

    public Arguments getArguments() {
        return arguments;
    }
}
