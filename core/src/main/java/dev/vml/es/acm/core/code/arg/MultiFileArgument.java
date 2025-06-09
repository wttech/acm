package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.ArgumentType;

import java.io.File;

public class MultiFileArgument extends AbstractFileArgument<File[]> {

    private Integer min;

    private Integer max;

    public MultiFileArgument(String name) {
        super(name, ArgumentType.MULTIFILE, File[].class);
    }

    public Integer getMin() {
        return min;
    }

    public void setMin(Integer min) {
        this.min = min;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }
}