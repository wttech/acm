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

    public User createUser(String id, String password, String path) {
        try {
            Principal principal = () -> id;
            if (StringUtils.isEmpty(password)) {
                password = PasswordUtils.generateRandomPassword();
            }
            return userManager.createUser(
                    id, StringUtils.defaultString(password, PasswordUtils.generateRandomPassword()), principal, path);
        } catch (RepositoryException e) {
            throw new AclException("Failed to create user", e);
        }
    }

    public Group createGroup(String id, String path, String externalId) {
        try {
            Principal principal = () -> id;
            Group group = userManager.createGroup(id, principal, path);
            if (StringUtils.isNotEmpty(externalId)) {
                group.setProperty("rep:externalId", valueFactory.createValue(externalId));
            }
            return group;
        } catch (RepositoryException e) {
            throw new AclException("Failed to create group", e);
        }
    }

    public User createSystemUser(String id, String path) {
        try {
            return userManager.createSystemUser(id, path);
        } catch (RepositoryException e) {
            throw new AclException("Failed to create system user", e);
        }
    }

    public void updateUser(User user, String password, Map<String, String> properties) {
        if (StringUtils.isNotEmpty(password)) {
            changePassword(user, password);
        }
        properties.forEach((name, value) -> setProperty(user, name, value));
    }

    public void updateGroup(Group group, Map<String, String> properties) {
        properties.forEach((name, value) -> setProperty(group, name, value));
    }

    public void deleteUser(User user) {
        try {
            Iterator<Group> groups = user.memberOf();
            while (groups.hasNext()) {
                groups.next().removeMember(user);
            }
            user.remove();
        } catch (RepositoryException e) {
            throw new AclException("Failed to delete user", e);
        }
    }

    public void deleteGroup(Group group) {
        try {
            group.remove();
        } catch (RepositoryException e) {
            throw new AclException("Failed to delete user", e);
        }
    }

    public boolean addToGroup(Authorizable authorizable, Group group) {
        try {
            if (!group.isMember(authorizable)) {
                return group.addMember(authorizable);
            }
            return false;
        } catch (RepositoryException e) {
            throw new AclException("Failed to add authorizable to group", e);
        }
    }

    public boolean removeFromGroup(Authorizable authorizable, Group group) {
        try {
            if (group.isMember(authorizable)) {
                return group.removeMember(authorizable);
            }
            return false;
        } catch (RepositoryException e) {
            throw new AclException("Failed to remove authorizable from group", e);
        }
    }

    public Authorizable getAuthorizable(String id) {
        try {
            return userManager.getAuthorizable(id);
        } catch (RepositoryException e) {
            throw new AclException("Failed to get authorizable", e);
        }
    }

    public <T extends Authorizable> T getAuthorizable(Class<T> authorizableClass, String id) {
        Authorizable authorizable = getAuthorizable(id);
        if (authorizable != null && !authorizableClass.isInstance(authorizable)) {
            throw new AclException(
                    String.format("Authorizable with id %s exists but is a %s", id, authorizableClass.getSimpleName()));
        }
        return authorizableClass.cast(authorizable);
    }

    public User getUser(String id) {
        return getAuthorizable(User.class, id);
    }

    public Group getGroup(String id) {
        return getAuthorizable(Group.class, id);
    }

    public void changePassword(User user, String password) {
        try {
            user.changePassword(password);
        } catch (RepositoryException e) {
            throw new AclException("Failed to change password", e);
        }
    }

    public void setProperty(Authorizable authorizable, String name, String value) {
        try {
            authorizable.setProperty(name, valueFactory.createValue(value));
        } catch (RepositoryException e) {
            throw new AclException("Failed to set property", e);
        }
    }

    public boolean removeProperty(Authorizable authorizable, String name) {
        try {
            if (authorizable.hasProperty(name)) {
                authorizable.removeProperty(name);
                return true;
            }
            return false;
        } catch (RepositoryException e) {
            throw new AclException("Failed to remove property", e);
        }
    }
}
