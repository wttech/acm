package dev.vml.es.acm.core.acl;

import dev.vml.es.acm.core.acl.authorizable.AclGroup;

public class RemoveAllMembersOptions {

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
