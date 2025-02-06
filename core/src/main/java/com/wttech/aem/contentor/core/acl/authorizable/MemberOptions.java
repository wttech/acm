package com.wttech.aem.contentor.core.acl.authorizable;

public class MemberOptions {

    private MyAuthorizable member;

    private String memberId;

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
