package com.wttech.aem.contentor.core.acl.check;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PermissionsOptions {

    private String id;

    private String path;

    private List<String> permissions = Collections.emptyList();

    private Map<String, Object> restrictions = new HashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public Map<String, Object> getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(Map<String, Object> restrictions) {
        this.restrictions = restrictions;
    }
}
