package com.vml.es.aem.acm.core.servlet;

import com.vml.es.aem.acm.core.code.Code;
import java.io.Serializable;
import java.util.Map;

public class ExecuteCodeInput implements Serializable {

    private String mode;

    private Boolean history;

    private Code code;

    private Map<String, Object> arguments;

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

    public Map<String, Object> getArguments() {
        return arguments;
    }
}
