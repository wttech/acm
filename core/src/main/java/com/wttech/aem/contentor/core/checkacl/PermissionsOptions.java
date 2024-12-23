package com.wttech.aem.contentor.core.checkacl;

import java.util.List;

public class PermissionsOptions {

    private String id;

    private String path;

    private List<String> permissions;
    private String glob;

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

    public String getGlob() {
        return glob;
    }

    public void setGlob(String glob) {
        this.glob = glob;
    }
}
