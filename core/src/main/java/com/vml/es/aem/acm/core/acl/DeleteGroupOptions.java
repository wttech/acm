package com.vml.es.aem.acm.core.acl;

import com.vml.es.aem.acm.core.acl.authorizable.AclGroup;

public class DeleteGroupOptions {

    private AclGroup group;

    private String id;

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
