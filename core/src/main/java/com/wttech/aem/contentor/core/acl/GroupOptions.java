package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.MyAuthorizable;

public class GroupOptions extends AuthorizableOptions {

    private MyAuthorizable group;

    private String groupId;

    public MyAuthorizable getGroup() {
        return group;
    }

    public void setGroup(MyAuthorizable group) {
        this.group = group;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}
