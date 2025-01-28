package com.wttech.aem.contentor.core.acl.utils;

import com.wttech.aem.contentor.core.acl.AclException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.ACE;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.Restriction;

public class CheckPermissionsManager {

    private final AccessControlManager accessControlManager;

    public CheckPermissionsManager(AccessControlManager accessControlManager) {
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
            Set<String> repoAggregatePermissions = determineAggregatePermissions(ace.getPrivileges());
            Set<String> aggregatePermissions = determineAggregatePermissions(permissions);
            result = repoAggregatePermissions.containsAll(aggregatePermissions);
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

    private Set<String> determineAggregatePermissions(Set<String> permissions) {
        return permissions.stream()
                .map(this::toPrivilege)
                .map(this::determineAggregatePermissions)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private Set<String> determineAggregatePermissions(Privilege[] privileges) {
        return Arrays.stream(privileges)
                .map(this::determineAggregatePermissions)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private Set<String> determineAggregatePermissions(Privilege privilege) {
        Set<Privilege> aggregatePrivileges = Collections.singleton(privilege);
        boolean isAggregate = aggregatePrivileges.stream().anyMatch(Privilege::isAggregate);
        while (isAggregate) {
            aggregatePrivileges = aggregatePrivileges.stream()
                    .map(this::getAggregatePermissions)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
            isAggregate = aggregatePrivileges.stream().anyMatch(Privilege::isAggregate);
        }
        return aggregatePrivileges.stream().map(Privilege::getName).collect(Collectors.toSet());
    }

    private Set<Privilege> getAggregatePermissions(Privilege privilege) {
        if (privilege.isAggregate()) {
            return Arrays.stream(privilege.getAggregatePrivileges()).collect(Collectors.toSet());
        }
        return Collections.singleton(privilege);
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
