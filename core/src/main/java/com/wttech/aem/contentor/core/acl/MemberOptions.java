package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.MyAuthorizable;
import com.wttech.aem.contentor.core.acl.authorizable.MyGroup;

public class MemberOptions {

    private MyGroup group;

    private String groupId;

    private MyAuthorizable member;

    private String memberId;

    public MyGroup getGroup() {
        return group;
    }

    public void setGroup(MyGroup group) {
        this.group = group;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public MyAuthorizable getMember() {
        return member;
    }

    public void setMember(MyAuthorizable member) {
        this.member = member;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }
}
