package com.wttech.aem.contentor.core.acl.authorizable;

import com.wttech.aem.contentor.core.acl.AclContext;
import java.util.Optional;

public class MemberOptions {

    private AclAuthorizable member;

    private String memberId;

    public AclAuthorizable determineMember(AclContext context) {
        return Optional.ofNullable(member).orElse(context.determineGroup(memberId));
    }

    public String determineMemberId() {
        return Optional.ofNullable(member).map(AclAuthorizable::getId).orElse(memberId);
    }

    public void setMember(AclAuthorizable member) {
        this.member = member;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }
}
