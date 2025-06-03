package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.Argument;
import dev.vml.es.acm.core.code.ArgumentType;

public class PathArgument extends Argument<String> {

    private String rootPath = "/";

    private boolean rootInclusive;

    public PathArgument(String name) {
        super(name, ArgumentType.PATH, String.class);
    }

    public String getRootPath() {
        return rootPath;
    }

    public boolean isRootInclusive() {
        return rootInclusive;
    }

    public void root(String path, boolean inclusive) {
        setRoot(path, inclusive);
    }

    public void setRoot(String path, boolean inclusive) {
        this.rootPath = path;
        this.rootInclusive = inclusive;
    }

    public void setRootPathInclusive(String path) {
        setRoot(path, true);
    }

    public void setRootPathExclusive(String path) {
        setRoot(path, false);
    }
}
