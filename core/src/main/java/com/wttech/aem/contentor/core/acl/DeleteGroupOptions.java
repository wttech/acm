package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclGroup;

public class DeleteGroupOptions {

    private AclGroup group;

    private String id;

    public DeleteGroupOptions() {}

    public DeleteGroupOptions(AclGroup group, String id) {
        this.group = group;
        this.id = id;
    }

    public AclGroup getGroup() {
        return group;
    }

    public void setGroup(AclGroup group) {
        this.group = group;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
