package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.Argument;
import dev.vml.es.acm.core.code.ArgumentType;

public class PathArgument extends Argument<String> {
    private String root;

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public PathArgument(String name) {
        super(name, ArgumentType.PATH, String.class);
    }
}
