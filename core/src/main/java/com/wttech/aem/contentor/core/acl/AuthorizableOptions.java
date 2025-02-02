package com.wttech.aem.contentor.core.acl;

public class AuthorizableOptions {

    private Object authorizable;

    private String id;

    public Object getAuthorizable() {
        return authorizable;
    }

    public void setAuthorizable(Object authorizable) {
        this.authorizable = authorizable;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
