package com.wttech.aem.contentor.core.acl.authorizable;

public class MemberOptions {

    private AclAuthorizable member;

    private String memberId;

    public MemberOptions() {}

    public MemberOptions(AclAuthorizable member, String memberId) {
        this.member = member;
        this.memberId = memberId;
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
