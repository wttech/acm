package com.vml.es.aem.acm.core.acl;

import com.vml.es.aem.acm.core.acl.authorizable.AclUser;

public class PasswordOptions extends com.vml.es.aem.acm.core.acl.authorizable.PasswordOptions {

    private AclUser user;

    private String userId;

    public AclUser getUser() {
        return user;
    }

    public void setUser(AclUser user) {
        this.user = user;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
