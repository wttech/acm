package com.wttech.aem.contentor.core.acl;

import org.apache.jackrabbit.api.security.user.Authorizable;

public class MemberOptions extends AuthorizableOptions {

    private Authorizable memberAuthorizable;

    private String memberId;

    public Authorizable getMemberAuthorizable() {
        return memberAuthorizable;
    }

    public void setMemberAuthorizable(Authorizable memberAuthorizable) {
        this.memberAuthorizable = memberAuthorizable;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }
}
