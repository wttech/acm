package com.wttech.aem.contentor.core.checkacl;

public class ExistsOptions {

    private String id;

    private String path;

    private Type type = Type.AUTHORIZABLE;

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

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void shouldBeUser() {
        this.type = Type.USER;
    }

    public void shouldBeSystemUser() {
        this.type = Type.SYSTEM_USER;
    }

    public void shouldBeGroup() {
        this.type = Type.GROUP;
    }

    public enum Type {
        AUTHORIZABLE,
        USER,
        SYSTEM_USER,
        GROUP
    }
}
