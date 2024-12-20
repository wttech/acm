package com.wttech.aem.contentor.core.acl;

import org.apache.jackrabbit.api.security.user.Authorizable;

public class AuthorizableOptions {

    private Authorizable authorizable;

    private String authorizableId;

    public Authorizable getAuthorizable() {
        return authorizable;
    }

    public void setAuthorizable(Authorizable authorizable) {
        this.authorizable = authorizable;
    }

    public String getAuthorizableId() {
        return authorizableId;
    }

    public void setAuthorizableId(String authorizableId) {
        this.authorizableId = authorizableId;
    }
}
