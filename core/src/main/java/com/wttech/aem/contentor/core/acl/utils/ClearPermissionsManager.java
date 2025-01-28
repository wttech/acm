package com.wttech.aem.contentor.core.acl.utils;

import com.wttech.aem.contentor.core.acl.AclException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.ACE;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AbstractAccessControlList;
import org.apache.jackrabbit.oak.spi.security.authorization.permission.PermissionConstants;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.Restriction;

public class ClearPermissionsManager {

    private static final String PERMISSION_STORE_PATH = "/jcr:system/rep:permissionStore/crx.default/";

    private final AccessControlManager accessControlManager;

    private final JackrabbitSession session;

    public ClearPermissionsManager(JackrabbitSession session, AccessControlManager accessControlManager) {
        this.session = session;
        this.accessControlManager = accessControlManager;
    }

    public boolean purge(Authorizable authorizable, String path, boolean strict) {
        if (strict) {
            return removeAll(authorizable, path);
        } else {
            return purge(authorizable, normalizePath(path));
        }
    }

    private boolean removeAll(Authorizable authorizable, String path) {
        try {
            JackrabbitAccessControlList jackrabbitAcl =
                    JackrabbitAccessControlListUtils.determineModifiableAcl(accessControlManager, path);
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

    private Set<String> getAccessControlledPaths(Authorizable authorizable) {
        try {
            Set<String> result = new HashSet<>();
            String path = PERMISSION_STORE_PATH + authorizable.getID();
            if (session.nodeExists(path)) {
                Node node = session.getNode(path);
                NodeIterator nodes = node.getNodes();
                while (nodes.hasNext()) {
                    node = nodes.nextNode();
                    if (node.hasProperty(PermissionConstants.REP_ACCESS_CONTROLLED_PATH)) {
                        result.add(node.getProperty(PermissionConstants.REP_ACCESS_CONTROLLED_PATH)
                                .getString());
                    }
                }
            } else {
                JackrabbitAccessControlManager jackrabbitAccessControlManager =
                        (JackrabbitAccessControlManager) accessControlManager;
                AccessControlPolicy[] accessControlPolicies =
                        jackrabbitAccessControlManager.getPolicies(authorizable.getPrincipal());
                for (AccessControlPolicy accessControlPolicy : accessControlPolicies) {
                    AbstractAccessControlList abstractAcl = (AbstractAccessControlList) accessControlPolicy;
                    List<? extends JackrabbitAccessControlEntry> jackrabbitAccessControlEntries =
                            abstractAcl.getEntries();
                    for (JackrabbitAccessControlEntry jackrabbitAccessControlEntry : jackrabbitAccessControlEntries) {
                        Set<Restriction> restrictions = ((ACE) jackrabbitAccessControlEntry).getRestrictions();
                        for (Restriction restriction : restrictions) {
                            if (Type.PATH.equals(restriction.getProperty().getType())) {
                                result.add(restriction.getProperty().getValue(Type.PATH));
                            }
                        }
                    }
                }
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
}
