package com.wttech.aem.acm.core.code.arg;

import com.wttech.aem.acm.core.code.Argument;
import com.wttech.aem.acm.core.code.ArgumentType;

public class StringArgument extends Argument<String> {

    public StringArgument(String name) {
        super(name, ArgumentType.STRING);
    }
}
