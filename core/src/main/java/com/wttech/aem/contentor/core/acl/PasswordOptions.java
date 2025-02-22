package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclUser;

public class PasswordOptions extends com.wttech.aem.contentor.core.acl.authorizable.PasswordOptions {

    private AclUser user;

    private String userId;

    public PasswordOptions() {}

    public PasswordOptions(AclUser user, String userId, String password) {
        this.user = user;
        this.userId = userId;
        setPassword(password);
    }

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

    public void setId(String id) {
        setUserId(id);
    }
}
