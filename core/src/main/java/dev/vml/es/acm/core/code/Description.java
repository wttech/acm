package dev.vml.es.acm.core.code;

import java.io.Serializable;

public class Description implements Serializable {

    private final Execution execution;

    private final Inputs inputs;

    public Description(Execution execution, Inputs inputs) {
        this.execution = execution;
        this.inputs = inputs;
    }

    public Execution getExecution() {
        return execution;
    }

    public Inputs getInputs() {
        return inputs;
    }
}
