package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclAuthorizable;
import com.wttech.aem.contentor.core.acl.authorizable.AclGroup;

public class MemberOptions extends com.wttech.aem.contentor.core.acl.authorizable.MemberOptions {

    private AclGroup group;

    private String groupId;

    public static MemberOptions of(AclGroup group, String groupId, AclAuthorizable member, String memberId) {
        MemberOptions options = new MemberOptions();
        options.group = group;
        options.groupId = groupId;
        options.setMember(member);
        options.setMemberId(memberId);
        return options;
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

    public void setId(String id) {
        setGroupId(id);
    }
}
