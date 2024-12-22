package com.wttech.aem.contentor.core.acl.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

public class PurgeManager {

    private static final String PERMISSION_STORE_PATH = "/jcr:system/rep:permissionStore/crx.default/";
    private final AccessControlManager accessControlManager;

    private final JackrabbitSession session;

    public PurgeManager(JackrabbitSession session, AccessControlManager accessControlManager) {
        this.session = session;
        this.accessControlManager = accessControlManager;
    }

    public void purge(Authorizable authorizable, String path, boolean strict) throws RepositoryException {
        if (strict) {
            removeAll(authorizable, path);
        } else {
            purge(authorizable, path);
        }
    }

    private void removeAll(Authorizable authorizable, String path) throws RepositoryException {
        JackrabbitAccessControlList jackrabbitAcl =
                JackrabbitAccessControlListUtil.determineModifiableAcl(accessControlManager, path);
        AccessControlEntry[] accessControlEntries = jackrabbitAcl.getAccessControlEntries();
        for (AccessControlEntry accessControlEntry : accessControlEntries) {
            if (Objects.equals(accessControlEntry.getPrincipal(), authorizable.getPrincipal())) {
                jackrabbitAcl.removeAccessControlEntry(accessControlEntry);
            }
        }
        accessControlManager.setPolicy(path, jackrabbitAcl);
    }

    private void purge(Authorizable authorizable, String path) throws RepositoryException {
        Set<String> accessControlledPaths = getAccessControlledPaths(authorizable);
        String normalizedPath = normalizePath(path);
        for (String parentPath : accessControlledPaths) {
            String normalizedParentPath = normalizePath(parentPath);
            boolean isUsersPermission = parentPath.startsWith(authorizable.getPath());
            if (StringUtils.startsWith(normalizedParentPath, normalizedPath) && !isUsersPermission) {
                removeAll(authorizable, parentPath);
            }
        }
    }

    private Set<String> getAccessControlledPaths(Authorizable authorizable) throws RepositoryException {
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
            JackrabbitAccessControlManager accessControlManager =
                    (JackrabbitAccessControlManager) session.getAccessControlManager();
            AccessControlPolicy[] accessControlPolicies = accessControlManager.getPolicies(authorizable.getPrincipal());
            for (AccessControlPolicy accessControlPolicy : accessControlPolicies) {
                AbstractAccessControlList abstractAccessControlList = (AbstractAccessControlList) accessControlPolicy;
                List<? extends JackrabbitAccessControlEntry> jackrabbitAccessControlEntries =
                        abstractAccessControlList.getEntries();
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
        return result;
    }

    private String normalizePath(String path) {
        return path + (path.endsWith("/") ? "" : "/");
    }
}
