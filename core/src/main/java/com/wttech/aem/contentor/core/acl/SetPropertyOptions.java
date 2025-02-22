package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclAuthorizable;

public class SetPropertyOptions extends com.wttech.aem.contentor.core.acl.authorizable.SetPropertyOptions {

    private AclAuthorizable authorizable;

    private String authorizableId;

    public SetPropertyOptions() {}

    public SetPropertyOptions(AclAuthorizable authorizable, String authorizableId, String relPath, String valie) {
        this.authorizable = authorizable;
        this.authorizableId = authorizableId;
        setRelPath(relPath);
        setValue(valie);
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
