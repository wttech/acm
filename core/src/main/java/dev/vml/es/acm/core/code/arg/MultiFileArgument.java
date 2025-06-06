package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.ArgumentType;

import java.io.File;

public class MultiFileArgument extends AbstractFileArgument<File[]> {

    public MultiFileArgument(String name) {
        super(name, ArgumentType.FILE, File[].class);
    }
}