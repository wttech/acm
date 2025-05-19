package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.Argument;
import dev.vml.es.acm.core.code.ArgumentType;

public class StringArgument extends Argument<String> {

    public StringArgument(String name) {
        super(name, ArgumentType.STRING);
    }
}
