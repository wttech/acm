package dev.vml.es.acm.core.servlet;

import dev.vml.es.acm.core.code.Code;
import dev.vml.es.acm.core.code.InputValues;

import java.io.Serializable;

public class QueueCodeInput implements Serializable {

    private Code code;

    private InputValues inputs;

    public QueueCodeInput() {
        // for deserialization
    }

    public Code getCode() {
        return code;
    }

    public InputValues getInputs() {
        return inputs;
    }
}
