package com.wttech.aem.acm.core.servlet;

import com.wttech.aem.acm.core.code.Code;
import java.io.Serializable;

public class QueueCodeInput implements Serializable {

    private Code code;

    public QueueCodeInput() {
        // for deserialization
    }

    public Code getCode() {
        return code;
    }
}
