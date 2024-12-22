package com.wttech.aem.contentor.core.acl.utils;

import com.wttech.aem.contentor.core.acl.AclException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class PermissionManager {

    private final AccessControlManager accessControlManager;

    private final ValueFactory valueFactory;

    public PermissionManager(AccessControlManager accessControlManager, ValueFactory valueFactory) {
        this.accessControlManager = accessControlManager;
        this.valueFactory = valueFactory;
    }

    public void applyPermissions(
            Authorizable authorizable,
            String path,
            List<String> permissions,
            Map<String, Object> restrictions,
            boolean allow)
            throws RepositoryException {
        updateAccessControlList(authorizable.getPrincipal(), path, permissions, restrictions, allow);
        if (permissions.contains("MODIFY")) {
            List<String> globModifyPermissions = Collections.singletonList("MODIFY_PAGE");
            Map<String, Object> globModifyRestrictions = new HashMap<>(restrictions);
            String preparedGlob = recalculateGlob((String) restrictions.get(AccessControlConstants.REP_GLOB));
            globModifyRestrictions.put(AccessControlConstants.REP_GLOB, preparedGlob + "*/jcr:content*");
            updateAccessControlList(
                    authorizable.getPrincipal(), path, globModifyPermissions, globModifyRestrictions, allow);
        }
    }

    private void updateAccessControlList(
            Principal principal, String path, List<String> permissions, Map<String, Object> restrictions, boolean allow)
            throws RepositoryException {
        JackrabbitAccessControlList jackrabbitAcl =
                JackrabbitAccessControlListUtil.determineModifiableAcl(accessControlManager, path);
        addEntry(jackrabbitAcl, principal, permissions, restrictions, allow);
        accessControlManager.setPolicy(path, jackrabbitAcl);
    }

    private void addEntry(
            JackrabbitAccessControlList jackrabbitAcl,
            Principal principal,
            List<String> permissions,
            Map<String, Object> restrictions,
            boolean allow)
            throws RepositoryException {
        List<Privilege> privileges = createPrivileges(permissions);
        Map<String, Value> singleValueRestrictions = getSingleValueRestrictions(jackrabbitAcl, restrictions);
        Map<String, Value[]> multiValueRestrictions = getMultiValueRestrictions(jackrabbitAcl, restrictions);
        jackrabbitAcl.addEntry(
                principal,
                privileges.toArray(new Privilege[] {}),
                allow,
                singleValueRestrictions,
                multiValueRestrictions);
    }

    private List<Privilege> createPrivileges(List<String> permissions) throws RepositoryException {
        List<Privilege> privileges = new ArrayList<>();
        for (String permission : permissions) {
            privileges.addAll(createPrivileges(accessControlManager, permission));
        }
        return privileges;
    }

    private List<Privilege> createPrivileges(AccessControlManager accessControlManager, String permission)
            throws RepositoryException {
        try {
            PrivilegeGroup privilegeGroup = PrivilegeGroup.fromTitle(permission);
            if (privilegeGroup != null) {
                return privilegeGroup.toPrivileges(accessControlManager);
            } else {
                return Collections.singletonList(accessControlManager.privilegeFromName(permission));
            }
        } catch (AccessControlException e) {
            throw new AclException("Unknown permission " + permission, e);
        }
    }

    private Map<String, Value> getSingleValueRestrictions(
            JackrabbitAccessControlList jackrabbitAcl, Map<String, Object> restrictions) throws RepositoryException {
        Map<String, Value> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : restrictions.entrySet()) {
            String key = entry.getKey();
            if (!jackrabbitAcl.isMultiValueRestriction(key)) {
                String value;
                if (entry.getValue() instanceof String) {
                    value = (String) entry.getValue();
                } else {
                    List<String> values = (List<String>) entry.getValue();
                    value = values.isEmpty() ? "" : values.get(0);
                }
                result.put(key, createValue(jackrabbitAcl, key, value));
            }
        }
        return result;
    }

    private Value createValue(JackrabbitAccessControlList jackrabbitAcl, String key, String value)
            throws RepositoryException {
        return valueFactory.createValue(value, jackrabbitAcl.getRestrictionType(key));
    }

    private Map<String, Value[]> getMultiValueRestrictions(
            JackrabbitAccessControlList jackrabbitAcl, Map<String, Object> restrictions) throws RepositoryException {
        Map<String, Value[]> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : restrictions.entrySet()) {
            String key = entry.getKey();
            if (jackrabbitAcl.isMultiValueRestriction(key)) {
                List<String> values;
                if (entry.getValue() instanceof String) {
                    String value = (String) entry.getValue();
                    values = value.isEmpty() ? Collections.emptyList() : Collections.singletonList(value);
                } else {
                    values = (List<String>) entry.getValue();
                }
                result.put(key, createValues(jackrabbitAcl, key, values));
            }
        }
        return result;
    }

    private Value[] createValues(JackrabbitAccessControlList jackrabbitAcl, String key, List<String> names)
            throws RepositoryException {
        Value[] values = new Value[names.size()];
        for (int index = 0; index < names.size(); index++) {
            values[index] = createValue(jackrabbitAcl, key, names.get(index));
        }
        return values;
    }

    private String recalculateGlob(String glob) {
        String preparedGlob = "";
        if (!StringUtils.isBlank(glob)) {
            preparedGlob = glob;
            if (StringUtils.endsWith(glob, "*")) {
                preparedGlob = StringUtils.substring(glob, 0, StringUtils.lastIndexOf(glob, '*'));
            }
        }
        return preparedGlob;
    }
}
