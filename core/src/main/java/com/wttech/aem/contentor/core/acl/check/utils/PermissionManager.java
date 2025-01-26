package com.wttech.aem.contentor.core.acl.check.utils;

import com.wttech.aem.contentor.core.acl.AclException;
import com.wttech.aem.contentor.core.acl.utils.JackrabbitAccessControlListUtils;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.ACE;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.Restriction;

public class PermissionManager {

    private final AccessControlManager accessControlManager;

    public PermissionManager(AccessControlManager accessControlManager) {
        this.accessControlManager = accessControlManager;
    }

    public boolean checkPermissions(
            Authorizable authorizable,
            String path,
            List<String> permissions,
            Map<String, Object> restrictions,
            boolean allow) {
        try {
            JackrabbitAccessControlList jackrabbitAcl =
                    JackrabbitAccessControlListUtils.determineModifiableAcl(accessControlManager, path);
            AccessControlEntry[] accessControlEntries = jackrabbitAcl.getAccessControlEntries();
            boolean result = false;
            for (AccessControlEntry accessControlEntry : accessControlEntries) {
                if (Objects.equals(accessControlEntry.getPrincipal(), authorizable.getPrincipal())) {
                    result |= checkACE((ACE) accessControlEntry, new HashSet<>(permissions), restrictions, allow);
                }
            }
            return result;
        } catch (RepositoryException e) {
            throw new AclException("Failed to check permissions", e);
        }
    }

    private boolean checkACE(ACE ace, Set<String> permissions, Map<String, Object> restrictions, boolean allow)
            throws RepositoryException {
        boolean result = allow == ace.isAllow();
        if (result) {
            Set<String> privileges =
                    Arrays.stream(ace.getPrivileges()).map(Privilege::getName).collect(Collectors.toSet());
            result = permissions.equals(privileges);
        }
        if (result) {
            Set<String> restrictionNames = ace.getRestrictions().stream()
                    .map(Restriction::getProperty)
                    .map(PropertyState::getName)
                    .collect(Collectors.toSet());
            result = restrictionNames.equals(restrictions.keySet());
        }
        for (Map.Entry<String, Object> entry : restrictions.entrySet()) {
            if (result) {
                Set<String> restrictionValues = new HashSet<>();
                for (Value restriction : ace.getRestrictions(entry.getKey())) {
                    restrictionValues.add(restriction.getString());
                }
                Set<String> values = new HashSet<>();
                if (entry.getValue() instanceof String) {
                    values.add((String) entry.getValue());
                } else if (entry.getValue() instanceof List) {
                    values.addAll((List<String>) entry.getValue());
                }
                result = restrictionValues.equals(values);
            }
        }
        return result;
    }
}
