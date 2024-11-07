package com.wttech.aem.migrator.core.api;

import com.wttech.aem.migrator.core.script.Code;
import java.io.Serializable;

public class ExecuteCodeInput implements Serializable {

    private Code code;

    public ExecuteCodeInput() {
        // for deserialization
    }

    public ExecuteCodeInput(Code code) {
        this.code = code;
    }

    public Code getCode() {
        return code;
    }
}
