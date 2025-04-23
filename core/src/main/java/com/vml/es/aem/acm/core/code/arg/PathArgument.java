package com.vml.es.aem.acm.core.code.arg;

import com.vml.es.aem.acm.core.code.Argument;
import com.vml.es.aem.acm.core.code.ArgumentType;

public class PathArgument extends Argument<String> {
    public PathArgument(String name) {
        super(name, ArgumentType.PATH);
    }
}
