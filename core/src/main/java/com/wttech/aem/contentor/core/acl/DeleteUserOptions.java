package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclAuthorizable;
import com.wttech.aem.contentor.core.acl.authorizable.AclUser;
import java.util.Optional;

public class DeleteUserOptions {

    private AclUser user;

    private String id;

    public AclUser determineUser(AclContext context) {
        return Optional.ofNullable(user).orElse(context.determineUser(id));
    }

    public String determineId() {
        return Optional.ofNullable(user).map(AclAuthorizable::getId).orElse(id);
    }

    public void setUser(AclUser user) {
        this.user = user;
    }

    public void setId(String id) {
        this.id = id;
    }
}
