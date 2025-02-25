package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclAuthorizable;

public class ClearOptions extends com.wttech.aem.contentor.core.acl.authorizable.ClearOptions {

    private AclAuthorizable authorizable;

    private String authorizableId;

    public static ClearOptions of(AclAuthorizable authorizable, String authorizableId, String path, boolean strict) {
        ClearOptions options = new ClearOptions();
        options.authorizable = authorizable;
        options.authorizableId = authorizableId;
        options.setPath(path);
        options.setStrict(strict);
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
