package com.wttech.aem.contentor.core.acl;

import org.apache.jackrabbit.api.security.user.Authorizable;

public class GroupOptions extends AuthorizableOptions {

    private Authorizable group;

    private String groupId;

    public Authorizable getGroup() {
        return group;
    }

    public void setGroup(Authorizable group) {
        this.group = group;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}
