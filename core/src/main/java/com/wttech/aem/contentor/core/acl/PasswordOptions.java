package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclUser;

public class PasswordOptions extends com.wttech.aem.contentor.core.acl.authorizable.PasswordOptions {

    private AclUser user;

    private String userId;

    public static PasswordOptions of(AclUser user, String userId, String password) {
        PasswordOptions options = new PasswordOptions();
        options.user = user;
        options.userId = userId;
        options.setPassword(password);
        return options;
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
