package com.wttech.aem.contentor.core.acl;

public class ClearOptions extends AuthorizableOptions {

    private String path;

    private boolean strict;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public void strictPath() {
        this.strict = true;
    }
}
