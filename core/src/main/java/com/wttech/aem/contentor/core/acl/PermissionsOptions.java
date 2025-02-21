package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclAuthorizable;
import java.util.List;
import java.util.Map;

public class PermissionsOptions extends com.wttech.aem.contentor.core.acl.authorizable.PermissionsOptions {

    private AclAuthorizable authorizable;

    private String authorizableId;

    public PermissionsOptions() {}

    public PermissionsOptions(
            AclAuthorizable authorizable,
            String authorizableId,
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            PermissionsOptions.Mode mode) {
        super(path, permissions, glob, types, properties, restrictions, mode);
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
}
