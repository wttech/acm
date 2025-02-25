package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclAuthorizable;

public class RemovePropertyOptions extends com.wttech.aem.contentor.core.acl.authorizable.RemovePropertyOptions {

    private AclAuthorizable authorizable;

    private String authorizableId;

    public static RemovePropertyOptions of(AclAuthorizable authorizable, String authorizableId, String relPath) {
        RemovePropertyOptions options = new RemovePropertyOptions();
        options.authorizable = authorizable;
        options.authorizableId = authorizableId;
        options.setRelPath(relPath);
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
