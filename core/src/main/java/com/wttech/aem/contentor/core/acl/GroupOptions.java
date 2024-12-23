package com.wttech.aem.contentor.core.acl;

import org.apache.jackrabbit.api.security.user.Authorizable;

public class GroupOptions extends AuthorizableOptions {

    private Authorizable groupAuthorizable;

    private String groupId;

    public Authorizable getGroupAuthorizable() {
        return groupAuthorizable;
    }

    public void setGroupAuthorizable(Authorizable groupAuthorizable) {
        this.groupAuthorizable = groupAuthorizable;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}
