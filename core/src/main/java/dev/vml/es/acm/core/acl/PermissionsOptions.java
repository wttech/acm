package dev.vml.es.acm.core.acl;

import dev.vml.es.acm.core.acl.authorizable.AclAuthorizable;

public class PermissionsOptions extends dev.vml.es.acm.core.acl.authorizable.PermissionsOptions {

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
}
