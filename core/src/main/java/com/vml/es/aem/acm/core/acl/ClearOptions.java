package com.vml.es.aem.acm.core.acl;

import com.vml.es.aem.acm.core.acl.authorizable.AclAuthorizable;

public class ClearOptions extends com.vml.es.aem.acm.core.acl.authorizable.ClearOptions {

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
