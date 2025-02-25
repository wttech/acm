package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclAuthorizable;
import com.wttech.aem.contentor.core.acl.authorizable.AclGroup;

public class GroupOptions extends com.wttech.aem.contentor.core.acl.authorizable.GroupOptions {

    private AclAuthorizable authorizable;

    private String authorizableId;

    public static GroupOptions of(AclAuthorizable authorizable, String authorizableId, AclGroup group, String groupId) {
        GroupOptions options = new GroupOptions();
        options.authorizable = authorizable;
        options.authorizableId = authorizableId;
        options.setGroup(group);
        options.setGroupId(groupId);
        return options;
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
