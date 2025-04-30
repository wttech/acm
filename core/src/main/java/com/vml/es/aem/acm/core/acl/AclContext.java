package com.vml.es.aem.acm.core.acl;

import com.vml.es.aem.acm.core.acl.authorizable.AclAuthorizable;
import com.vml.es.aem.acm.core.acl.authorizable.AclGroup;
import com.vml.es.aem.acm.core.acl.authorizable.AclUser;
import com.vml.es.aem.acm.core.acl.utils.AuthorizableManager;
import com.vml.es.aem.acm.core.acl.utils.PermissionsManager;
import com.vml.es.aem.acm.core.repo.Repo;
import java.util.Optional;
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
            this.compositeNodeStore = new Repo(resourceResolver).isCompositeNodeStore();
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
            return user == null ? null : new AclUser(user, user.getID(), this);
        } catch (RepositoryException e) {
            throw new AclException("Failed to get authorizable ID", e);
        }
    }

    public AclUser determineUser(String id) {
        return determineUser(authorizableManager.getUser(id));
    }

    public AclUser determineUser(AclUser user, String id) {
        return Optional.ofNullable(user).orElse(determineUser(id));
    }

    public AclGroup determineGroup(Group group) {
        try {
            return group == null ? null : new AclGroup(group, group.getID(), this);
        } catch (RepositoryException e) {
            throw new AclException("Failed to get authorizable ID", e);
        }
    }

    public AclGroup determineGroup(String id) {
        return determineGroup(authorizableManager.getGroup(id));
    }

    public AclGroup determineGroup(AclGroup group, String id) {
        return Optional.ofNullable(group).orElse(determineGroup(id));
    }

    public AclAuthorizable determineAuthorizable(AclAuthorizable authorizable, String id) {
        return Optional.ofNullable(authorizable).orElse(determineAuthorizable(id));
    }

    private AclAuthorizable determineAuthorizable(String id) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        if (authorizable == null) {
            return null;
        } else if (authorizable.isGroup()) {
            return new AclGroup((Group) authorizable, id, this);
        } else {
            return new AclUser((User) authorizable, id, this);
        }
    }

    public String determineId(AclAuthorizable authorizable, String id) {
        return Optional.ofNullable(authorizable).map(AclAuthorizable::getId).orElse(id);
    }
}
