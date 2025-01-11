package com.wttech.aem.contentor.core.acl.check.utils;

import com.day.cq.security.util.CqActions;
import com.wttech.aem.contentor.core.acl.AclException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.user.Authorizable;

public class PermissionManager {

    private final JackrabbitSession session;

    public PermissionManager(JackrabbitSession session) {
        this.session = session;
    }

    public boolean checkPermissions(
            Authorizable authorizable, String path, List<String> permissions, String glob, boolean allow) {
        try {
            Set<Principal> authorizablesToCheck = getAuthorizablesToCheck(authorizable);
            CqActions actions = new CqActions(session);
            List<String> privilegesToCheck = preparePrivilegesToCheck(permissions);
            if (glob == null) {
                return checkPermissionsForPath(authorizablesToCheck, actions, privilegesToCheck, path, allow);
            } else {
                return checkPermissionsForGlob(authorizablesToCheck, actions, privilegesToCheck, path, glob, allow);
            }
        } catch (RepositoryException e) {
            throw new AclException("Failed to check permissions", e);
        }
    }

    private boolean checkPermissionsForGlob(
            Set<Principal> authorizablesToCheck,
            CqActions actions,
            List<String> privilegesToCheck,
            String path,
            String glob,
            boolean allow) {
        List<String> subpaths = getAllSubpaths(session, path);
        Pattern pattern = Pattern.compile(path + StringUtils.replace(glob, "*", ".*"));
        boolean foundMatch = false;
        boolean failed = false;
        for (String subpath : subpaths) {
            if (pattern.matcher(subpath).matches()) {
                foundMatch = true;
                failed = checkPermissionsForPath(authorizablesToCheck, actions, privilegesToCheck, subpath, allow);
                if (failed) {
                    break;
                }
            }
        }
        return foundMatch && !failed;
    }

    private boolean checkPermissionsForPath(
            Set<Principal> authorizablesToCheck,
            CqActions actions,
            List<String> privilegesToCheck,
            String subpath,
            boolean allow) {
        try {
            Collection<String> allowedActions = actions.getAllowedActions(subpath, authorizablesToCheck);
            boolean containsAll = allowedActions.containsAll(privilegesToCheck);
            return (!containsAll && allow) || (containsAll && !allow);
        } catch (RepositoryException e) {
            throw new AclException("Failed to check permissions for path", e);
        }
    }

    private List<String> getAllSubpaths(Session session, String path) {
        try {
            Node node = session.getNode(path);
            return new ArrayList<>(crawl(node));
        } catch (RepositoryException e) {
            throw new AclException("Failed to get all subpaths", e);
        }
    }

    private List<String> crawl(Node node) {
        try {
            List<String> paths = new ArrayList<>();
            paths.add(node.getPath());
            for (NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
                paths.addAll(crawl(iter.nextNode()));
            }
            return paths;
        } catch (RepositoryException e) {
            throw new AclException("Failed to crawl", e);
        }
    }

    private Set<Principal> getAuthorizablesToCheck(Authorizable authorizable) {
        try {
            Set<Principal> principals = new HashSet<>();
            Principal principal = authorizable.getPrincipal();
            principals.add(principal);
            for (PrincipalIterator it = session.getPrincipalManager().getGroupMembership(principal); it.hasNext(); ) {
                principals.add(it.nextPrincipal());
            }
            return principals;
        } catch (RepositoryException e) {
            throw new AclException("Failed to get authorizables to check", e);
        }
    }

    private List<String> preparePrivilegesToCheck(List<String> permissions) {
        return permissions.stream().map(String::toLowerCase).collect(Collectors.toList());
    }
}
