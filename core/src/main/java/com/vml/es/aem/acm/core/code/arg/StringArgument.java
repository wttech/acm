package com.vml.es.aem.acm.core.code.arg;

import com.vml.es.aem.acm.core.code.Argument;
import com.vml.es.aem.acm.core.code.ArgumentType;

public class StringArgument extends Argument<String> {

    public StringArgument(String name) {
        super(name, ArgumentType.STRING);
    }
}
