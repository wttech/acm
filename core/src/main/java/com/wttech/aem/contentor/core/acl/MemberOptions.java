package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclAuthorizable;
import com.wttech.aem.contentor.core.acl.authorizable.AclGroup;

public class MemberOptions extends com.wttech.aem.contentor.core.acl.authorizable.MemberOptions {

    private AclGroup group;

    private String groupId;

    public MemberOptions() {}

    public MemberOptions(AclGroup group, String groupId, AclAuthorizable member, String memberId) {
        super(member, memberId);
        this.group = group;
        this.groupId = groupId;
    }

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
