package com.wttech.aem.contentor.core.acl;

public class MemberOptions {

    private AclGroup group;

    private String groupId;

    private AclAuthorizable member;

    private String memberId;

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

    public AclAuthorizable getMember() {
        return member;
    }

    public void setMember(AclAuthorizable member) {
        this.member = member;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }
}
