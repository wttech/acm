package com.wttech.aem.contentor.core.acl.utils;

import com.wttech.aem.contentor.core.acl.AclException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants;

public class ApplyPermissionsManager {

    private final AccessControlManager accessControlManager;

    private final ValueFactory valueFactory;

    public ApplyPermissionsManager(AccessControlManager accessControlManager, ValueFactory valueFactory) {
        this.accessControlManager = accessControlManager;
        this.valueFactory = valueFactory;
    }

    public void applyPermissions(
            Authorizable authorizable,
            String path,
            List<String> permissions,
            Map<String, Object> restrictions,
            boolean allow) {
        try {
            updateAccessControlList(authorizable.getPrincipal(), path, permissions, restrictions, allow);
            if (permissions.contains("MODIFY")) {
                List<String> modifyPermissions = Collections.singletonList("MODIFY_PAGE");
                Map<String, Object> modifyRestrictions = new HashMap<>(restrictions);
                modifyRestrictions.computeIfPresent(
                        AccessControlConstants.REP_GLOB, (key, glob) -> recalculateGlob((String) glob));
                updateAccessControlList(
                        authorizable.getPrincipal(), path, modifyPermissions, modifyRestrictions, allow);
            }
        } catch (RepositoryException e) {
            throw new AclException("Failed to apply permissions", e);
        }
    }

    private void updateAccessControlList(
            Principal principal,
            String path,
            List<String> permissions,
            Map<String, Object> restrictions,
            boolean allow) {
        try {
            JackrabbitAccessControlList jackrabbitAcl =
                    JackrabbitAccessControlListUtils.determineModifiableAcl(accessControlManager, path);
            addEntry(jackrabbitAcl, principal, permissions, restrictions, allow);
            accessControlManager.setPolicy(path, jackrabbitAcl);
        } catch (RepositoryException e) {
            throw new AclException("Failed to update access control list", e);
        }
    }

    private void addEntry(
            JackrabbitAccessControlList jackrabbitAcl,
            Principal principal,
            List<String> permissions,
            Map<String, Object> restrictions,
            boolean allow) {
        try {
            List<Privilege> privileges = createPrivileges(permissions);
            Map<String, Value[]> multiValueRestrictions = new HashMap<>();
            Map<String, Value> singleValueRestrictions = new HashMap<>();
            for (Map.Entry<String, Object> entry : restrictions.entrySet()) {
                String key = entry.getKey();
                Value[] values = createValues(jackrabbitAcl, entry);
                if (jackrabbitAcl.isMultiValueRestriction(key)) {
                    multiValueRestrictions.put(key, values);
                } else {
                    singleValueRestrictions.put(key, values[0]);
                }
            }
            jackrabbitAcl.addEntry(
                    principal,
                    privileges.toArray(new Privilege[] {}),
                    allow,
                    singleValueRestrictions,
                    multiValueRestrictions);
        } catch (RepositoryException e) {
            throw new AclException("Failed to add entry to acl", e);
        }
    }

    private List<Privilege> createPrivileges(List<String> permissions) {
        return permissions.stream().map(this::toPrivilege).collect(Collectors.toList());
    }

    private Value[] createValues(JackrabbitAccessControlList jackrabbitAcl, Map.Entry<String, Object> entry) {
        try {
            List<String> names = Collections.singletonList("");
            if (entry.getValue() instanceof String) {
                names = Collections.singletonList((String) entry.getValue());
            } else if (entry.getValue() instanceof List) {
                names = (List<String>) entry.getValue();
            }
            Value[] values = new Value[names.size()];
            int type = jackrabbitAcl.getRestrictionType(entry.getKey());
            for (int index = 0; index < names.size(); index++) {
                values[index] = valueFactory.createValue(names.get(index), type);
            }
            return values;
        } catch (RepositoryException e) {
            throw new AclException("Failed to create values", e);
        }
    }

    private String recalculateGlob(String glob) {
        if (StringUtils.endsWith(glob, "*")) {
            return StringUtils.substring(glob, 0, StringUtils.lastIndexOf(glob, '*')) + "*/jcr:content*";
        }
        return glob + "*/jcr:content*";
    }

    private Privilege toPrivilege(String permission) {
        try {
            return accessControlManager.privilegeFromName(permission);
        } catch (AccessControlException e) {
            throw new AclException("Unknown permission " + permission, e);
        } catch (RepositoryException e) {
            throw new AclException("Failed to create privilege", e);
        }
    }
}
