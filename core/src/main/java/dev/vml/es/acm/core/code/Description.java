package dev.vml.es.acm.core.code;

import java.io.Serializable;

public class Description implements Serializable {

    private final Execution execution;

    private final Inputs arguments;

    public Description(Execution execution, Inputs arguments) {
        this.execution = execution;
        this.arguments = arguments;
    }

    public Execution getExecution() {
        return execution;
    }

    public Inputs getArguments() {
        return arguments;
    }
}
