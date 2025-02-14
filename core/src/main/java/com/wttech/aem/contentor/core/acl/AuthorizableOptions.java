package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.MyAuthorizable;

public class AuthorizableOptions {

    private MyAuthorizable authorizable;

    private String authorizableId;

    public MyAuthorizable getAuthorizable() {
        return authorizable;
    }

    public void setAuthorizable(MyAuthorizable authorizable) {
        this.authorizable = authorizable;
    }

    public String getAuthorizableId() {
        return authorizableId;
    }

    public void setAuthorizableId(String authorizableId) {
        this.authorizableId = authorizableId;
    }
}
