package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclAuthorizable;
import com.wttech.aem.contentor.core.acl.authorizable.AclUser;
import java.util.Optional;

public class PasswordOptions extends com.wttech.aem.contentor.core.acl.authorizable.PasswordOptions {

    private AclUser user;

    private String userId;

    public AclUser determineUser(AclContext context) {
        return Optional.ofNullable(user).orElse(context.determineUser(userId));
    }

    public String determineUserId() {
        return Optional.ofNullable(user).map(AclAuthorizable::getId).orElse(userId);
    }

    public void setUser(AclUser user) {
        this.user = user;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
