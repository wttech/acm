package com.wttech.aem.contentor.core.acl;

import org.apache.jackrabbit.api.security.user.Authorizable;

public class PropertyOptions {

    private Authorizable authorizable;

    private String id;

    private String key;

    private String value;

    public Authorizable getAuthorizable() {
        return authorizable;
    }

    public void setAuthorizable(Authorizable authorizable) {
        this.authorizable = authorizable;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
