package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclAuthorizable;

public class PermissionsOptions extends com.wttech.aem.contentor.core.acl.authorizable.PermissionsOptions {

    private AclAuthorizable authorizable;

    private String authorizableId;

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
