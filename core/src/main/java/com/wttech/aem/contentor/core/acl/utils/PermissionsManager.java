package com.wttech.aem.contentor.core.acl.utils;

import java.util.List;
import java.util.Map;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlManager;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;

public class PermissionsManager {

    private final ApplyPermissionsManager applyPermissionsManager;

    private final CheckPermissionsManager checkPermissionsManager;

    private final ClearPermissionsManager clearPermissionsManager;

    public PermissionsManager(
            JackrabbitSession session, AccessControlManager accessControlManager, ValueFactory valueFactory) {
        this.applyPermissionsManager = new ApplyPermissionsManager(accessControlManager, valueFactory);
        this.checkPermissionsManager = new CheckPermissionsManager(accessControlManager);
        this.clearPermissionsManager = new ClearPermissionsManager(session, accessControlManager);
    }

    public void apply(
            Authorizable authorizable,
            String path,
            List<String> permissions,
            Map<String, Object> restrictions,
            boolean allow) {
        applyPermissionsManager.applyPermissions(authorizable, path, permissions, restrictions, allow);
    }

    public boolean check(
            Authorizable authorizable,
            String path,
            List<String> permissions,
            Map<String, Object> restrictions,
            boolean allow) {
        return checkPermissionsManager.checkPermissions(authorizable, path, permissions, restrictions, allow);
    }

    public boolean clear(Authorizable authorizable, String path, boolean strict) {
        return clearPermissionsManager.purge(authorizable, path, strict);
    }
}
