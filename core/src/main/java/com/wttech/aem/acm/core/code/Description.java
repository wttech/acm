package com.wttech.aem.acm.core.code;

import java.io.Serializable;

public class Description implements Serializable {

    private final Executable executable;

    private final Arguments arguments;

    public Description(Executable executable, Arguments arguments) {
        this.executable = executable;
        this.arguments = arguments;
    }

    public Executable getExecutable() {
        return executable;
    }

    public Arguments getArguments() {
        return arguments;
    }
}
