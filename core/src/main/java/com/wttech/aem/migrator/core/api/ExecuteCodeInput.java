package com.wttech.aem.migrator.core.api;

import com.wttech.aem.migrator.core.script.Code;

import java.io.Serializable;

public class ExecuteCodeInput implements Serializable {

    private String mode;

    private Code code;

    public ExecuteCodeInput() {
        // for deserialization
    }

    public String getMode() {
        return mode;
    }

    public Code getCode() {
        return code;
    }
}
