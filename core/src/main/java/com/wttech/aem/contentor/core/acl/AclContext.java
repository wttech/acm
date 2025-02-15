package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.MyAuthorizable;
import com.wttech.aem.contentor.core.acl.authorizable.MyGroup;
import com.wttech.aem.contentor.core.acl.authorizable.MyUser;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(AclContext.class);

    private final ResourceResolver resourceResolver;

    private final AuthorizableManager authorizableManager;

    private final PermissionsManager permissionsManager;

    private final boolean compositeNodeStore;

    public AclContext(ResourceResolver resourceResolver) {
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
            throw new AclException("Failed to initialize acl context", e);
        }
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

    public MyUser determineUser(User user) {
        try {
            String id = "";
            if (user != null) {
                id = user.getID();
            }
            return new MyUser(user, id, this);
        } catch (RepositoryException e) {
            throw new AclException("Failed to get authorizable ID", e);
        }
    }

    public MyUser determineUser(String id) {
        User user = authorizableManager.getUser(id);
        return new MyUser(user, id, this);
    }

    public MyUser determineUser(MyUser user, String id) {
        if (user == null) {
            user = determineUser(id);
        }
        return user;
    }

    public MyGroup determineGroup(Group group) {
        try {
            String id = "";
            if (group != null) {
                id = group.getID();
            }
            return new MyGroup(group, id, this);
        } catch (RepositoryException e) {
            throw new AclException("Failed to get authorizable ID", e);
        }
    }

    public MyGroup determineGroup(String id) {
        Group group = authorizableManager.getGroup(id);
        return new MyGroup(group, id, this);
    }

    public MyGroup determineGroup(MyGroup group, String id) {
        if (group == null) {
            group = determineGroup(id);
        }
        return group;
    }

    public MyAuthorizable determineAuthorizable(AuthorizableOptions options) {
        return determineAuthorizable(options.getAuthorizable(), options.getAuthorizableId());
    }

    public MyAuthorizable determineAuthorizable(MyAuthorizable authorizable, String id) {
        if (authorizable == null) {
            authorizable = determineAuthorizable(id);
        }
        return authorizable;
    }

    public MyAuthorizable determineAuthorizable(String id) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        return new MyAuthorizable(authorizable, id, this);
    }

    public void logResult(MyAuthorizable authorizable, String messagePattern, Object... args) {
        String newMessagePattern = String.format("[%s] %s", authorizable.getId(), messagePattern);
        LOGGER.info(newMessagePattern, args);
    }
}
