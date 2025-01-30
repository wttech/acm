package com.wttech.aem.contentor.core.acl.utils;

import com.wttech.aem.contentor.core.acl.AclException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;

public class AuthorizableManager {

    private final JackrabbitSession session;

    private final UserManager userManager;

    private final ValueFactory valueFactory;

    public AuthorizableManager(JackrabbitSession session, UserManager userManager, ValueFactory valueFactory) {
        this.session = session;
        this.userManager = userManager;
        this.valueFactory = valueFactory;
    }

    public User createUser(String id, String password, String path) {
        try {
            Principal principal = () -> id;
            if (StringUtils.isEmpty(password)) {
                password = PasswordUtils.generateRandomPassword();
            }
            User user = userManager.createUser(
                    id, StringUtils.defaultString(password, PasswordUtils.generateRandomPassword()), principal, path);
            save();
            return user;
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
            save();
            return group;
        } catch (RepositoryException e) {
            throw new AclException("Failed to create group", e);
        }
    }

    public User createSystemUser(String id, String path) {
        try {
            User user = userManager.createSystemUser(id, path);
            save();
            return user;
        } catch (RepositoryException e) {
            throw new AclException("Failed to create system user", e);
        }
    }

    public void updateUser(User user, String password, Map<String, String> properties) {
        if (StringUtils.isNotEmpty(password)) {
            changePassword(user, password);
        }
        properties.forEach((relPath, value) -> setProperty(user, relPath, value));
    }

    public void updateGroup(Group group, Map<String, String> properties) {
        properties.forEach((relPath, value) -> setProperty(group, relPath, value));
    }

    public void deleteAuthorizable(Authorizable authorizable) {
        try {
            Iterator<Group> groups = authorizable.memberOf();
            while (groups.hasNext()) {
                groups.next().removeMember(authorizable);
            }
            authorizable.remove();
            save();
        } catch (RepositoryException e) {
            throw new AclException("Failed to delete authorizable", e);
        }
    }

    public boolean addMember(Group group, Authorizable authorizable) {
        try {
            boolean result = false;
            if (!group.isMember(authorizable)) {
                result = group.addMember(authorizable);
            }
            if (result) {
                save();
            }
            return result;
        } catch (RepositoryException e) {
            throw new AclException("Failed to add authorizable to group", e);
        }
    }

    public boolean removeMember(Group group, Authorizable authorizable) {
        try {
            boolean result = false;
            if (group.isMember(authorizable)) {
                result = group.removeMember(authorizable);
            }
            if (result) {
                save();
            }
            return result;
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

    public boolean testPassword(Authorizable authorizable, String password) {
        try {
            Repository repository = session.getRepository();
            Credentials credentials = new SimpleCredentials(authorizable.getID(), password.toCharArray());
            try {
                repository.login(credentials).logout();
            } catch (LoginException e) {
                return false;
            }
            return true;
        } catch (RepositoryException e) {
            throw new AclException("Failed to test password", e);
        }
    }

    public void changePassword(User user, String password) {
        try {
            user.changePassword(password);
            save();
        } catch (RepositoryException e) {
            throw new AclException("Failed to change password", e);
        }
    }

    public List<String> getProperty(Authorizable authorizable, String relPath) {
        try {
            List<String> result = null;
            Value[] values = authorizable.getProperty(relPath);
            if (values != null) {
                result = new ArrayList<>();
                for (Value value : values) {
                    result.add(value.getString());
                }
            }
            return result;
        } catch (RepositoryException e) {
            throw new AclException("Failed to get property", e);
        }
    }

    public void setProperty(Authorizable authorizable, String relPath, String value) {
        try {
            authorizable.setProperty(relPath, valueFactory.createValue(value));
            save();
        } catch (RepositoryException e) {
            throw new AclException("Failed to set property", e);
        }
    }

    public boolean removeProperty(Authorizable authorizable, String relPath) {
        try {
            if (authorizable.hasProperty(relPath)) {
                authorizable.removeProperty(relPath);
                save();
                return true;
            }
            return false;
        } catch (RepositoryException e) {
            throw new AclException("Failed to remove property", e);
        }
    }

    private void save() throws RepositoryException {
        session.save();
    }
}
