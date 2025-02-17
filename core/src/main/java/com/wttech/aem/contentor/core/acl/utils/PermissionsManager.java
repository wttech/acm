package com.wttech.aem.contentor.core.acl.utils;

import com.wttech.aem.contentor.core.acl.AclException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.ACE;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AbstractAccessControlList;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants;
import org.apache.jackrabbit.oak.spi.security.authorization.permission.PermissionConstants;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.Restriction;

public class PermissionsManager {

    private static final String PERMISSION_STORE_PATH = "/jcr:system/rep:permissionStore/crx.default/";

    private final JackrabbitSession session;

    private final AccessControlManager accessControlManager;

    private final ValueFactory valueFactory;

    public PermissionsManager(
            JackrabbitSession session, AccessControlManager accessControlManager, ValueFactory valueFactory) {
        this.session = session;
        this.accessControlManager = accessControlManager;
        this.valueFactory = valueFactory;
    }

    public void apply(
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
            session.save();
        } catch (RepositoryException e) {
            throw new AclException("Failed to apply permissions", e);
        }
    }

    public boolean clear(Authorizable authorizable, String path, boolean strict) {
        if (strict) {
            return removeAll(authorizable, path);
        } else {
            return purge(authorizable, normalizePath(path));
        }
    }

    public boolean check(
            Authorizable authorizable,
            String path,
            List<String> permissions,
            Map<String, Object> restrictions,
            boolean allow) {
        try {
            JackrabbitAccessControlList jackrabbitAcl = JcrAclUtils.determineModifiableAcl(accessControlManager, path);
            AccessControlEntry[] accessControlEntries = jackrabbitAcl.getAccessControlEntries();
            boolean result = false;
            for (AccessControlEntry accessControlEntry : accessControlEntries) {
                if (Objects.equals(accessControlEntry.getPrincipal(), authorizable.getPrincipal())) {
                    result |= checkAce((ACE) accessControlEntry, new HashSet<>(permissions), restrictions, allow);
                }
            }
            return result;
        } catch (RepositoryException e) {
            throw new AclException("Failed to check permissions", e);
        }
    }

    private void updateAccessControlList(
            Principal principal,
            String path,
            List<String> permissions,
            Map<String, Object> restrictions,
            boolean allow) {
        try {
            JackrabbitAccessControlList jackrabbitAcl = JcrAclUtils.determineModifiableAcl(accessControlManager, path);
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
            throw new AclException(String.format("Failed to determine privilege for '%s'", permission), e);
        }
    }

    private boolean removeAll(Authorizable authorizable, String path) {
        try {
            JackrabbitAccessControlList jackrabbitAcl = JcrAclUtils.determineModifiableAcl(accessControlManager, path);
            AccessControlEntry[] accessControlEntries = jackrabbitAcl.getAccessControlEntries();
            boolean result = false;
            for (AccessControlEntry accessControlEntry : accessControlEntries) {
                if (Objects.equals(accessControlEntry.getPrincipal(), authorizable.getPrincipal())) {
                    jackrabbitAcl.removeAccessControlEntry(accessControlEntry);
                    result = true;
                }
            }
            if (result) {
                accessControlManager.setPolicy(path, jackrabbitAcl);
                session.save();
            }
            return result;
        } catch (RepositoryException e) {
            throw new AclException("Failed to remove all privileges from path", e);
        }
    }

    private boolean purge(Authorizable authorizable, String path) {
        Set<String> accessControlledPaths = getAccessControlledPaths(authorizable);
        boolean result = false;
        for (String parentPath : accessControlledPaths) {
            if (StringUtils.startsWith(parentPath, path)) {
                result |= removeAll(authorizable, parentPath);
            }
        }
        return result;
    }

    private Set<String> getAccessControlledPathsFromJcr(String path) {
        try {
            Set<String> result = new HashSet<>();
            Node node = session.getNode(path);
            NodeIterator nodes = node.getNodes();
            while (nodes.hasNext()) {
                node = nodes.nextNode();
                if (node.hasProperty(PermissionConstants.REP_ACCESS_CONTROLLED_PATH)) {
                    result.add(node.getProperty(PermissionConstants.REP_ACCESS_CONTROLLED_PATH)
                            .getString());
                }
            }
            return result;
        } catch (RepositoryException e) {
            throw new AclException("Failed to get access controlled paths from jcr", e);
        }
    }

    private Set<String> getAccessControlledPathsByApi(Authorizable authorizable) {
        try {
            Set<String> result = new HashSet<>();
            JackrabbitAccessControlManager jackrabbitAccessControlManager =
                    (JackrabbitAccessControlManager) accessControlManager;
            AccessControlPolicy[] accessControlPolicies =
                    jackrabbitAccessControlManager.getPolicies(authorizable.getPrincipal());
            for (AccessControlPolicy accessControlPolicy : accessControlPolicies) {
                AbstractAccessControlList abstractAcl = (AbstractAccessControlList) accessControlPolicy;
                List<? extends JackrabbitAccessControlEntry> jackrabbitAccessControlEntries = abstractAcl.getEntries();
                for (JackrabbitAccessControlEntry jackrabbitAccessControlEntry : jackrabbitAccessControlEntries) {
                    Set<Restriction> restrictions = ((ACE) jackrabbitAccessControlEntry).getRestrictions();
                    for (Restriction restriction : restrictions) {
                        if (Type.PATH.equals(restriction.getProperty().getType())) {
                            result.add(restriction.getProperty().getValue(Type.PATH));
                        }
                    }
                }
            }
            return result;
        } catch (RepositoryException e) {
            throw new AclException("Failed to get access controlled paths from api", e);
        }
    }

    private Set<String> getAccessControlledPaths(Authorizable authorizable) {
        try {
            Set<String> result;
            String path = PERMISSION_STORE_PATH + authorizable.getID();
            if (session.nodeExists(path)) {
                result = getAccessControlledPathsFromJcr(path);
            } else {
                result = getAccessControlledPathsByApi(authorizable);
            }
            String authorizablePath = authorizable.getPath();
            return result.stream()
                    .filter(controlledPath -> !StringUtils.equals(controlledPath, authorizablePath))
                    .map(this::normalizePath)
                    .collect(Collectors.toSet());
        } catch (RepositoryException e) {
            throw new AclException("Failed to get access controlled paths", e);
        }
    }

    private String normalizePath(String path) {
        return path + (path.endsWith("/") ? "" : "/");
    }

    private boolean checkAce(ACE ace, Set<String> permissions, Map<String, Object> restrictions, boolean allow)
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
}
