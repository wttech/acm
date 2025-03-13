package com.wttech.aem.acm.core.servlet;

import com.wttech.aem.acm.core.code.Code;
import java.io.Serializable;

public class ExecuteCodeInput implements Serializable {

    private String mode;

    private Code code;

    private Boolean history;

    public ExecuteCodeInput() {
        // for deserialization
    }

    public String getMode() {
        return mode;
    }

    public Code getCode() {
        return code;
    }

    public Boolean getHistory() {
        return history;
    }
}
