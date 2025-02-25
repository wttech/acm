package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclGroup;

public class DeleteGroupOptions {

    private AclGroup group;

    private String id;

    public static DeleteGroupOptions of(AclGroup group, String id) {
        DeleteGroupOptions options = new DeleteGroupOptions();
        options.group = group;
        options.id = id;
        return options;
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
