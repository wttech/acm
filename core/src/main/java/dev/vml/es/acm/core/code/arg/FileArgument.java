package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.ArgumentType;

import java.io.File;

public class FileArgument extends AbstractFileArgument<File> {

    public FileArgument(String name) {
        super(name, ArgumentType.FILE, File.class);
    }
}
