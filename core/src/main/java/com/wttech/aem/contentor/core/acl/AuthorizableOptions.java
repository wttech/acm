package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclAuthorizable;
import java.util.Optional;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;

public class AuthorizableOptions {

    private AclAuthorizable authorizable;

    private String authorizableId;

    public AclAuthorizable determineAuthorizable(AclContext context) {
        return Optional.ofNullable(authorizable).orElseGet(() -> {
            Authorizable authorizable = context.getAuthorizableManager().getAuthorizable(authorizableId);
            if (authorizable == null) {
                return null;
            } else if (authorizable.isGroup()) {
                return context.determineGroup((Group) authorizable);
            } else {
                return context.determineUser((User) authorizable);
            }
        });
    }

    public String determineAuthorizableId() {
        return Optional.ofNullable(authorizable).map(AclAuthorizable::getId).orElse(authorizableId);
    }

    public void setAuthorizable(AclAuthorizable authorizable) {
        this.authorizable = authorizable;
    }

    public void setAuthorizableId(String authorizableId) {
        this.authorizableId = authorizableId;
    }
}
