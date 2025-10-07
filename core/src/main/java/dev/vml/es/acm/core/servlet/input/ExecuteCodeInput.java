package dev.vml.es.acm.core.servlet.input;

import dev.vml.es.acm.core.code.Code;
import dev.vml.es.acm.core.code.InputValues;
import java.io.Serializable;

public class ExecuteCodeInput implements Serializable {

    private String mode;

    private Boolean history;

    private Code code;

    private InputValues inputs;

    public ExecuteCodeInput() {
        // for deserialization
    }

    public String getMode() {
        return mode;
    }

    public Boolean getHistory() {
        return history;
    }

    public Code getCode() {
        return code;
    }

    public InputValues getInputs() {
        return inputs;
    }
}
