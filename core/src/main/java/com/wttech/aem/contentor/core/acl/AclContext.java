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

    protected final Logger logger;

    private final ResourceResolver resourceResolver;

    private final AuthorizableManager authorizableManager;

    private final PermissionsManager permissionsManager;

    private final boolean compositeNodeStore;

    public AclContext(ResourceResolver resourceResolver) {
        this.logger = LoggerFactory.getLogger(AclContext.class);

        try {
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
            String id = "";
            if (user != null) {
                id = user.getID();
            }
            return new AclUser(user, id, this);
        } catch (RepositoryException e) {
            throw new AclException("Failed to get authorizable ID", e);
        }
    }

    public AclUser determineUser(String id) {
        User user = authorizableManager.getUser(id);
        return new AclUser(user, id, this);
    }

    public AclUser determineUser(AclUser user, String id) {
        if (user == null) {
            user = determineUser(id);
        }
        return user;
    }

    public AclGroup determineGroup(Group group) {
        try {
            String id = "";
            if (group != null) {
                id = group.getID();
            }
            return new AclGroup(group, id, this);
        } catch (RepositoryException e) {
            throw new AclException("Failed to get authorizable ID", e);
        }
    }

    public AclGroup determineGroup(String id) {
        Group group = authorizableManager.getGroup(id);
        return new AclGroup(group, id, this);
    }

    public AclGroup determineGroup(AclGroup group, String id) {
        if (group == null) {
            group = determineGroup(id);
        }
        return group;
    }

    public AclAuthorizable determineAuthorizable(AuthorizableOptions options) {
        return determineAuthorizable(options.getAuthorizable(), options.getAuthorizableId());
    }

    public AclAuthorizable determineAuthorizable(AclAuthorizable authorizable, String id) {
        if (authorizable == null) {
            authorizable = determineAuthorizable(id);
        }
        return authorizable;
    }

    public AclAuthorizable determineAuthorizable(String id) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        return new AclAuthorizable(authorizable, id, this);
    }
}
