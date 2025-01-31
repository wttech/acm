package com.wttech.aem.contentor.core.acl.authorizable;

import org.apache.jackrabbit.api.security.user.Authorizable;

public class MemberOptions {

    private Authorizable member;

    private String memberId;

    public Authorizable getMember() {
        return member;
    }

    public void setMember(Authorizable member) {
        this.member = member;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }
}
