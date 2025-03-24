package com.wttech.aem.acm.core.code.arg;

import com.wttech.aem.acm.core.code.Argument;
import com.wttech.aem.acm.core.code.ArgumentType;
import java.util.Map;

public class SelectArgument extends Argument<Map<String, String>> {

    public SelectArgument(String name) {
        super(name, ArgumentType.SELECT);
    }
}
