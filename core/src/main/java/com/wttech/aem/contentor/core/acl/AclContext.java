package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.MyAuthorizable;
import com.wttech.aem.contentor.core.acl.authorizable.MyGroup;
import com.wttech.aem.contentor.core.acl.authorizable.MyUser;
import com.wttech.aem.contentor.core.acl.utils.AuthorizableManager;
import com.wttech.aem.contentor.core.acl.utils.PermissionsManager;
import com.wttech.aem.contentor.core.acl.utils.RuntimeUtils;
import java.io.IOException;
import java.io.OutputStream;
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
import org.slf4j.helpers.MessageFormatter;

public class AclContext {

    private final ResourceResolver resourceResolver;

    private final AuthorizableManager authorizableManager;

    private final PermissionsManager permissionsManager;

    private final boolean compositeNodeStore;

    private final OutputStream out;

    public AclContext(ResourceResolver resourceResolver, OutputStream out) {
        try {
            JackrabbitSession session = (JackrabbitSession) resourceResolver.adaptTo(Session.class);
            UserManager userManager = session.getUserManager();
            AccessControlManager accessControlManager = session.getAccessControlManager();
            ValueFactory valueFactory = session.getValueFactory();
            this.resourceResolver = resourceResolver;
            this.authorizableManager = new AuthorizableManager(session, userManager, valueFactory);
            this.permissionsManager = new PermissionsManager(session, accessControlManager, valueFactory);
            this.compositeNodeStore = RuntimeUtils.determineCompositeNodeStore(session);
            this.out = out;
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

    public MyUser determineUser(AuthorizableOptions options) {
        return determineUser(options.getAuthorizable(), options.getId());
    }

    public MyUser determineUser(Authorizable authorizable) {
        try {
            return determineAuthorizable(authorizable, authorizable.getID(), MyUser.class);
        } catch (RepositoryException e) {
            throw new AclException("Failed to get authorizable ID", e);
        }
    }

    public MyUser determineUser(String id) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        return determineAuthorizable(authorizable, id, MyUser.class);
    }

    public MyUser determineUser(MyAuthorizable authorizable, String id) {
        MyUser user;
        if (authorizable == null) {
            user = determineUser(id);
        } else {
            user = determineUser(authorizable.get());
        }
        return user;
    }

    public MyGroup determineGroup(AuthorizableOptions options) {
        return determineGroup(options.getAuthorizable(), options.getId());
    }

    public MyGroup determineGroup(Authorizable authorizable) {
        try {
            return determineAuthorizable(authorizable, authorizable.getID(), MyGroup.class);
        } catch (RepositoryException e) {
            throw new AclException("Failed to get authorizable ID", e);
        }
    }

    public MyGroup determineGroup(String id) {
        Group authorizable = authorizableManager.getGroup(id);
        return determineAuthorizable(authorizable, id, MyGroup.class);
    }

    public MyGroup determineGroup(MyAuthorizable authorizable, String id) {
        MyGroup group;
        if (authorizable == null) {
            group = determineGroup(id);
        } else {
            group = determineGroup(authorizable.get());
        }
        return group;
    }

    public MyAuthorizable determineAuthorizable(AuthorizableOptions options) {
        return determineAuthorizable(options.getAuthorizable(), options.getId());
    }

    public MyAuthorizable determineAuthorizable(Authorizable authorizable) {
        try {
            return determineAuthorizable(authorizable, authorizable.getID(), MyAuthorizable.class);
        } catch (RepositoryException e) {
            throw new AclException("Failed to get authorizable ID", e);
        }
    }

    public MyAuthorizable determineAuthorizable(String id) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        return determineAuthorizable(authorizable, id, MyAuthorizable.class);
    }

    public MyAuthorizable determineAuthorizable(MyAuthorizable authorizable, String id) {
        if (authorizable == null) {
            authorizable = determineAuthorizable(id);
        } else {
            authorizable = determineAuthorizable(authorizable.get());
        }
        return authorizable;
    }

    private <T extends MyAuthorizable> T determineAuthorizable(Authorizable authorizable, String id, Class<T> clazz) {
        MyAuthorizable result;
        if (authorizable == null) {
            if (clazz.equals(MyGroup.class)) {
                result = new MyGroup(null, id, this);
            } else if (clazz.equals(MyUser.class)) {
                result = new MyUser(null, id, this);
            } else {
                result = new MyAuthorizable(null, id, this);
            }
        } else if (authorizable.isGroup()) {
            result = new MyGroup((Group) authorizable, id, this);
        } else {
            result = new MyUser((User) authorizable, id, this);
        }
        if (result.get() != null && !clazz.isInstance(result)) {
            throw new AclException(String.format(
                    "Authorizable with id %s exists but is a %s",
                    id, result.get().getClass().getSimpleName()));
        }
        return clazz.cast(result);
    }

    public void logResult(MyAuthorizable authorizable, String messagePattern, Object... args) {
        try {
            String newMessagePattern = String.format("[%s] %s\n", authorizable.getId(), messagePattern);
            String message = MessageFormatter.format(newMessagePattern, args).getMessage();
            out.write(message.getBytes());
        } catch (IOException e) {
            throw new AclException("Failed to log result", e);
        }
    }
}
