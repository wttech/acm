package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclAuthorizable;
import com.wttech.aem.contentor.core.acl.authorizable.AclGroup;
import java.util.Optional;

public class DeleteGroupOptions {

    private AclGroup group;

    private String id;

    public AclGroup determineGroup(AclContext context) {
        return Optional.ofNullable(group).orElse(context.determineGroup(id));
    }

    public String determineId() {
        return Optional.ofNullable(group).map(AclAuthorizable::getId).orElse(id);
    }

    public void setGroup(AclGroup group) {
        this.group = group;
    }

    public void setId(String id) {
        this.id = id;
    }
}
