package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclAuthorizable;
import java.util.List;
import java.util.Map;

public class PermissionsOptions extends com.wttech.aem.contentor.core.acl.authorizable.PermissionsOptions {

    private AclAuthorizable authorizable;

    private String authorizableId;

    public static PermissionsOptions of(
            AclAuthorizable authorizable,
            String authorizableId,
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            PermissionsOptions.Mode mode) {
        PermissionsOptions options = new PermissionsOptions();
        options.authorizable = authorizable;
        options.authorizableId = authorizableId;
        options.setPath(path);
        options.setPermissions(permissions);
        options.setGlob(glob);
        options.setTypes(types);
        options.setProperties(properties);
        options.setRestrictions(restrictions);
        options.setMode(mode);
        return options;
    }

    public static PermissionsOptions of(
            AclAuthorizable authorizable,
            String authorizableId,
            String path,
            List<String> permissions,
            String glob,
            Map<String, Object> restrictions) {
        PermissionsOptions options = new PermissionsOptions();
        options.authorizable = authorizable;
        options.authorizableId = authorizableId;
        options.setPath(path);
        options.setPermissions(permissions);
        options.setGlob(glob);
        options.setRestrictions(restrictions);
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
