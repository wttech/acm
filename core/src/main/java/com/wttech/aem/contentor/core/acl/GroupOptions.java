package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclAuthorizable;
import java.util.Optional;

public class GroupOptions extends com.wttech.aem.contentor.core.acl.authorizable.GroupOptions {

    private AclAuthorizable authorizable;

    private String authorizableId;

    public AclAuthorizable determineAuthorizable(AclContext context) {
        return Optional.ofNullable(authorizable).orElse(context.determineAuthorizable(authorizableId));
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
