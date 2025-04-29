package com.vml.es.aem.acm.core.code.arg;

import com.vml.es.aem.acm.core.code.Argument;
import com.vml.es.aem.acm.core.code.ArgumentType;

public class ColorArgument extends Argument<String> {
    public ColorArgument(String name) {
        super(name, ArgumentType.COLOR);
    }
}
