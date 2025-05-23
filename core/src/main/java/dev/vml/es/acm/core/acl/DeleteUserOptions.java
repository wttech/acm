package dev.vml.es.acm.core.acl;

import dev.vml.es.acm.core.acl.authorizable.AclUser;

public class DeleteUserOptions {

    private AclUser user;

    private String id;

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
