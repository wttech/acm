package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclAuthorizable;

public class AuthorizableOptions {

    private AclAuthorizable authorizable;

    private String authorizableId;

    public static AuthorizableOptions of(AclAuthorizable authorizable, String authorizableId) {
        AuthorizableOptions options = new AuthorizableOptions();
        options.authorizable = authorizable;
        options.authorizableId = authorizableId;
        return options;
    }

    public AclAuthorizable getAuthorizable() {
        return authorizable;
    }

    public void setAuthorizable(AclAuthorizable authorizable) {
        this.authorizable = authorizable;
    }

    public String getAuthorizableId() {
        return authorizableId;
    }

    public void setAuthorizableId(String authorizableId) {
        this.authorizableId = authorizableId;
    }

    public void setId(String id) {
        setAuthorizableId(id);
    }
}
