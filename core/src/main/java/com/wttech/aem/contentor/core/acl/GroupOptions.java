package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclAuthorizable;
import com.wttech.aem.contentor.core.acl.authorizable.AclGroup;

public class GroupOptions extends com.wttech.aem.contentor.core.acl.authorizable.GroupOptions {

    private AclAuthorizable authorizable;

    private String authorizableId;

    public GroupOptions() {}

    public GroupOptions(AclAuthorizable authorizable, String authorizableId, AclGroup group, String groupId) {
        super(group, groupId);
        this.authorizable = authorizable;
        this.authorizableId = authorizableId;
    }

    public AclAuthorizable getAuthorizable() {
        return authorizable;
    }

    public void setAuthorizable(AclAuthorizable authorizable) {
        this.authorizable = authorizable;
    }

    public String getAuthorizableId() {
        return authorizableId;
    }

    public void setAuthorizableId(String authorizableId) {
        this.authorizableId = authorizableId;
    }

    public void setId(String id) {
        setAuthorizableId(id);
    }
}
