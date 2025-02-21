package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclAuthorizable;

public class AuthorizableOptions {

    private AclAuthorizable authorizable;

    private String authorizableId;

    public AuthorizableOptions() {}

    public AuthorizableOptions(AclAuthorizable authorizable, String authorizableId) {
        this.authorizable = authorizable;
        this.authorizableId = authorizableId;
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
}
