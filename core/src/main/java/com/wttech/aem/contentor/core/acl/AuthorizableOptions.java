package com.wttech.aem.contentor.core.acl;

import org.apache.jackrabbit.api.security.user.Authorizable;

public class AuthorizableOptions {

    private Authorizable authorizable;

    private String id;

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
}
