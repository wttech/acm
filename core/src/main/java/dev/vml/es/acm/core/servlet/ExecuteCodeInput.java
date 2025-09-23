package dev.vml.es.acm.core.servlet;

import dev.vml.es.acm.core.code.Code;
import java.io.Serializable;
import java.util.Map;

public class ExecuteCodeInput implements Serializable {

    private String mode;

    private Boolean history;

    private Code code;

    @SuppressWarnings("java:S1948")
    private Map<String, Object> inputs;

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

    public Map<String, Object> getInputs() {
        return inputs;
    }
}
