package com.vml.es.aem.acm.core.acl.authorizable;

public class MemberOptions {

    private AclAuthorizable member;

    private String memberId;

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
