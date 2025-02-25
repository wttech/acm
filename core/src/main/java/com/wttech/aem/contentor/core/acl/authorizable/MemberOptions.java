package com.wttech.aem.contentor.core.acl.authorizable;

public class MemberOptions {

    private AclAuthorizable member;

    private String memberId;

    public static MemberOptions of(AclAuthorizable member, String memberId) {
        MemberOptions options = new MemberOptions();
        options.member = member;
        options.memberId = memberId;
        return options;
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
