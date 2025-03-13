package com.wttech.aem.acm.core.acl.authorizable;

public class ClearOptions {

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
