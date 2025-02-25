package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclUser;

public class DeleteUserOptions {

    private AclUser user;

    private String id;

    public static DeleteUserOptions of(AclUser user, String id) {
        DeleteUserOptions options = new DeleteUserOptions();
        options.user = user;
        options.id = id;
        return options;
    }

    public AclUser getUser() {
        return user;
    }

    public void setUser(AclUser user) {
        this.user = user;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
