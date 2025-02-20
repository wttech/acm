package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclAuthorizable;
import com.wttech.aem.contentor.core.acl.authorizable.AclGroup;
import com.wttech.aem.contentor.core.acl.authorizable.AclUser;
import com.wttech.aem.contentor.core.acl.utils.AuthorizableManager;
import com.wttech.aem.contentor.core.acl.utils.PermissionsManager;
import com.wttech.aem.contentor.core.acl.utils.RuntimeUtils;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlManager;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AclContext {

    private final Logger logger;

    private final ResourceResolver resourceResolver;

    private final AuthorizableManager authorizableManager;

    private final PermissionsManager permissionsManager;

    private final boolean compositeNodeStore;

    public AclContext(ResourceResolver resourceResolver) {
        try {
            this.logger = LoggerFactory.getLogger(AclContext.class);
            JackrabbitSession session = (JackrabbitSession) resourceResolver.adaptTo(Session.class);
            UserManager userManager = session.getUserManager();
            AccessControlManager accessControlManager = session.getAccessControlManager();
            ValueFactory valueFactory = session.getValueFactory();
            this.resourceResolver = resourceResolver;
            this.authorizableManager = new AuthorizableManager(session, userManager, valueFactory);
            this.permissionsManager = new PermissionsManager(session, accessControlManager, valueFactory);
            this.compositeNodeStore = RuntimeUtils.determineCompositeNodeStore(session);
        } catch (RepositoryException e) {
            throw new AclException("Cannot access repository while obtaining ACL context!", e);
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public AuthorizableManager getAuthorizableManager() {
        return authorizableManager;
    }

    public PermissionsManager getPermissionsManager() {
        return permissionsManager;
    }

    public boolean isCompositeNodeStore() {
        return compositeNodeStore;
    }

    public AclUser determineUser(User user) {
        try {
            if (user == null) {
                return null;
            }
            return new AclUser(user, user.getID(), this);
        } catch (RepositoryException e) {
            throw new AclException("Failed to get authorizable ID", e);
        }
    }

    public AclUser determineUser(String id) {
        User user = authorizableManager.getUser(id);
        return determineUser(user);
    }

    public AclGroup determineGroup(Group group) {
        try {
            if (group == null) {
                return null;
            }
            return new AclGroup(group, group.getID(), this);
        } catch (RepositoryException e) {
            throw new AclException("Failed to get authorizable ID", e);
        }
    }

    public AclGroup determineGroup(String id) {
        Group group = authorizableManager.getGroup(id);
        return determineGroup(group);
    }

    public AclAuthorizable determineAuthorizable(String id) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        if (authorizable == null) {
            return null;
        }
        return new AclAuthorizable(authorizable, id, this);
    }
}
