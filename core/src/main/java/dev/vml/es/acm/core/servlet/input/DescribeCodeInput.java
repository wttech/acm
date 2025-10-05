package dev.vml.es.acm.core.servlet.input;

import dev.vml.es.acm.core.code.Code;
import java.io.Serializable;

public class DescribeCodeInput implements Serializable {

    private Code code;

    public DescribeCodeInput() {
        // for deserialization
    }

    public Code getCode() {
        return code;
    }
}
