package dev.vml.es.acm.core.acl.check;

public class MemberOptions extends AuthorizableOptions {

    private String memberId;

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }
}
