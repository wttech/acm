package com.wttech.aem.contentor.core.checkacl.utils;

import com.day.cq.security.util.CqActions;
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
            Authorizable authorizable, String path, List<String> permissions, String glob, boolean allow)
            throws RepositoryException {
        Set<Principal> authorizablesToCheck = getAuthorizablesToCheck(authorizable);
        CqActions actions = new CqActions(session);
        List<String> privilegesToCheck = preparePrivilegesToCheck(permissions);
        if (glob == null) {
            return checkPermissionsForPath(authorizablesToCheck, actions, privilegesToCheck, path, allow);
        } else {
            return checkPermissionsForGlob(authorizablesToCheck, actions, privilegesToCheck, path, glob, allow);
        }
    }

    private boolean checkPermissionsForGlob(
            Set<Principal> authorizablesToCheck,
            CqActions actions,
            List<String> privilegesToCheck,
            String path,
            String glob,
            boolean allow)
            throws RepositoryException {
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
            boolean allow)
            throws RepositoryException {
        Collection<String> allowedActions = actions.getAllowedActions(subpath, authorizablesToCheck);
        boolean containsAll = allowedActions.containsAll(privilegesToCheck);
        return (!containsAll && allow) || (containsAll && !allow);
    }

    private List<String> getAllSubpaths(Session session, String path) throws RepositoryException {
        Node node = session.getNode(path);
        return new ArrayList<>(crawl(node));
    }

    private List<String> crawl(Node node) throws RepositoryException {
        List<String> paths = new ArrayList<>();
        paths.add(node.getPath());
        for (NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
            paths.addAll(crawl(iter.nextNode()));
        }
        return paths;
    }

    private Set<Principal> getAuthorizablesToCheck(Authorizable authorizable) throws RepositoryException {
        Set<Principal> principals = new HashSet<>();
        Principal principal = authorizable.getPrincipal();
        principals.add(principal);
        for (PrincipalIterator it = session.getPrincipalManager().getGroupMembership(principal); it.hasNext(); ) {
            principals.add(it.nextPrincipal());
        }
        return principals;
    }

    private List<String> preparePrivilegesToCheck(List<String> permissions) {
        return permissions.stream().map(String::toLowerCase).collect(Collectors.toList());
    }
}
