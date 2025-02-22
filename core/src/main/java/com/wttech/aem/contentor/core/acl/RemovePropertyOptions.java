package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclAuthorizable;

public class RemovePropertyOptions extends com.wttech.aem.contentor.core.acl.authorizable.RemovePropertyOptions {

    private AclAuthorizable authorizable;

    private String authorizableId;

    public RemovePropertyOptions() {}

    public RemovePropertyOptions(AclAuthorizable authorizable, String authorizableId, String relPath) {
        this.authorizable = authorizable;
        this.authorizableId = authorizableId;
        setRelPath(relPath);
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
