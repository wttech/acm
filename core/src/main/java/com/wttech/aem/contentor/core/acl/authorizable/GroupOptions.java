package com.wttech.aem.contentor.core.acl.authorizable;

import com.wttech.aem.contentor.core.acl.AclGroup;

public class GroupOptions {

    private AclGroup group;

    private String groupId;

    public AclGroup getGroup() {
        return group;
    }

    public void setGroup(AclGroup group) {
        this.group = group;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}
