package com.wttech.aem.contentor.core.acl;

import java.util.Collection;
import java.util.Collections;

public class RestrictionOptions extends AuthorizableOptions {

    // TODO strongly natively-typed here, provide string-accepting utility setters
    private Collection<String> permissions = Collections.emptyList();

    // TODO strongly natively-typed here, provide string-accepting utility setters
    private Collection<String> restrictions = Collections.emptyList();

    private String path;

    private PathMode pathMode;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public PathMode getPathMode() {
        return pathMode;
    }

    public void setPathMode(PathMode mode) {
        this.pathMode = mode;
    }

    public void skipPathMissing() {
        pathMode = PathMode.SKIP_MISSING;
    }

    public void failPathMissing() {
        pathMode = PathMode.FAIL_MISSING;
    }

    public Collection<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Collection<String> permissions) {
        this.permissions = permissions;
    }

    public Collection<String> getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(Collection<String> restrictions) {
        this.restrictions = restrictions;
    }

    public enum PathMode {
        FAIL_MISSING,
        SKIP_MISSING
    }
}
