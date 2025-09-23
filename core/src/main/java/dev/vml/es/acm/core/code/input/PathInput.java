package dev.vml.es.acm.core.code.input;

import dev.vml.es.acm.core.code.Input;
import dev.vml.es.acm.core.code.InputType;

public class PathInput extends Input<String> {

    private String rootPath = "/";

    private boolean rootInclusive;

    public PathInput(String name) {
        super(name, InputType.PATH, String.class);
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
