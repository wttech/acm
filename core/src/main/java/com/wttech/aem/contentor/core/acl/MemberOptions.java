package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclAuthorizable;
import com.wttech.aem.contentor.core.acl.authorizable.AclGroup;
import java.util.Optional;

public class MemberOptions extends com.wttech.aem.contentor.core.acl.authorizable.MemberOptions {

    private AclGroup group;

    private String groupId;

    public AclGroup determineGroup(AclContext context) {
        return Optional.ofNullable(group).orElse(context.determineGroup(groupId));
    }

    public String determineGroupId() {
        return Optional.ofNullable(group).map(AclAuthorizable::getId).orElse(groupId);
    }

    public void setGroup(AclGroup group) {
        this.group = group;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}
