package com.wttech.aem.contentor.core.acl.utils;

import com.wttech.aem.contentor.core.acl.AclException;
import java.security.Principal;
import java.util.Iterator;
import java.util.Map;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;

public class AuthorizableManager {

    private final UserManager userManager;

    private final ValueFactory valueFactory;

    public AuthorizableManager(UserManager userManager, ValueFactory valueFactory) {
        this.userManager = userManager;
        this.valueFactory = valueFactory;
    }

    public User createUser(String id, String password, String path) throws AclException {
        try {
            Principal principal = () -> id;
            return userManager.createUser(id, password, principal, path);
        } catch (RepositoryException e) {
            throw new AclException(e);
        }
    }

    public Group createGroup(String id, String path, String externalId) throws AclException {
        try {
            Principal principal = () -> id;
            Group group = userManager.createGroup(id, principal, path);
            if (StringUtils.isEmpty(externalId)) {
                group.setProperty("rep:externalId", valueFactory.createValue(externalId));
            }
            return group;
        } catch (RepositoryException e) {
            throw new AclException(e);
        }
    }

    public User createSystemUser(String id, String path) throws AclException {
        try {
            return userManager.createSystemUser(id, path);
        } catch (RepositoryException e) {
            throw new AclException(e);
        }
    }

    public void updateUser(User user, String password, Map<String, String> properties) throws AclException {
        try {
            if (StringUtils.isEmpty(password)) {
                user.changePassword(password);
            }
            for (Map.Entry<String, String> property : properties.entrySet()) {
                user.setProperty(property.getKey(), valueFactory.createValue(property.getValue()));
            }
        } catch (RepositoryException e) {
            throw new AclException(e);
        }
    }

    public void updateGroup(Group group, Map<String, String> properties) throws AclException {
        try {
            for (Map.Entry<String, String> property : properties.entrySet()) {
                group.setProperty(property.getKey(), valueFactory.createValue(property.getValue()));
            }
        } catch (RepositoryException e) {
            throw new AclException(e);
        }
    }

    public void removeUser(User user) throws AclException {
        try {
            Iterator<Group> groups = user.memberOf();
            while (groups.hasNext()) {
                groups.next().removeMember(user);
            }
            user.remove();
        } catch (RepositoryException e) {
            throw new AclException(e);
        }
    }

    public void removeGroup(Group group) throws AclException {
        try {
            group.remove();
        } catch (RepositoryException e) {
            throw new AclException(e);
        }
    }

    public Authorizable getAuthorizable(String id) throws AclException {
        try {
            return userManager.getAuthorizable(id);
        } catch (RepositoryException e) {
            throw new AclException(e);
        }
    }

    public <T extends Authorizable> T getAuthorizable(Class<T> authorizableClass, String id) throws AclException {
        Authorizable authorizable = getAuthorizable(id);
        if (authorizable != null && !authorizableClass.isInstance(authorizable)) {
            throw new AclException(String.format("Authorizable with id %s exists but is a %s", id, authorizableClass.getSimpleName()));
        }
        return authorizableClass.cast(authorizable);
    }

    public User getUser(String id) throws AclException {
        return getAuthorizable(User.class, id);
    }

    public Group getGroup(String id) throws AclException {
        return getAuthorizable(Group.class, id);
    }
}
