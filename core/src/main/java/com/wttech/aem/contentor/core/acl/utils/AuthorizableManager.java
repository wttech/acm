package com.wttech.aem.contentor.core.acl.utils;

import com.wttech.aem.contentor.core.acl.AclException;
import com.wttech.aem.contentor.core.acl.AclResult;
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

    public User createUser(String id, String password, String path) throws RepositoryException {
        Principal principal = () -> id;
        if (StringUtils.isEmpty(password)) {
            password = PasswordUtils.generateRandomPassword();
        }
        return userManager.createUser(
                id, StringUtils.defaultString(password, PasswordUtils.generateRandomPassword()), principal, path);
    }

    public Group createGroup(String id, String path, String externalId) throws RepositoryException {
        Principal principal = () -> id;
        Group group = userManager.createGroup(id, principal, path);
        if (StringUtils.isNotEmpty(externalId)) {
            group.setProperty("rep:externalId", valueFactory.createValue(externalId));
        }
        return group;
    }

    public User createSystemUser(String id, String path) throws RepositoryException {
        return userManager.createSystemUser(id, path);
    }

    public void updateAuthorizable(Authorizable authorizable, Map<String, String> properties)
            throws RepositoryException {
        for (Map.Entry<String, String> property : properties.entrySet()) {
            authorizable.setProperty(property.getKey(), valueFactory.createValue(property.getValue()));
        }
    }

    public void updateUser(User user, String password, Map<String, String> properties) throws RepositoryException {
        if (StringUtils.isNotEmpty(password)) {
            user.changePassword(password);
        }
        updateAuthorizable(user, properties);
    }

    public void updateGroup(Group group, Map<String, String> properties) throws RepositoryException {
        updateAuthorizable(group, properties);
    }

    public void removeUser(User user) throws RepositoryException {
        Iterator<Group> groups = user.memberOf();
        while (groups.hasNext()) {
            groups.next().removeMember(user);
        }
        user.remove();
    }

    public void removeGroup(Group group) throws RepositoryException {
        group.remove();
    }

    public AclResult addToGroup(Authorizable authorizable, Group group) throws RepositoryException {
        if (!group.isMember(authorizable)) {
            return group.addMember(authorizable) ? AclResult.OK : AclResult.FAILED;
        }
        return AclResult.SKIPPED;
    }

    public AclResult removeFromGroup(Authorizable authorizable, Group group) throws RepositoryException {
        if (group.isMember(authorizable)) {
            return group.removeMember(authorizable) ? AclResult.OK : AclResult.FAILED;
        }
        return AclResult.SKIPPED;
    }

    public Authorizable getAuthorizable(String id) throws RepositoryException {
        return userManager.getAuthorizable(id);
    }

    public <T extends Authorizable> T getAuthorizable(Class<T> authorizableClass, String id)
            throws RepositoryException {
        Authorizable authorizable = getAuthorizable(id);
        if (authorizable != null && !authorizableClass.isInstance(authorizable)) {
            throw new AclException(
                    String.format("Authorizable with id %s exists but is a %s", id, authorizableClass.getSimpleName()));
        }
        return authorizableClass.cast(authorizable);
    }

    public User getUser(String id) throws RepositoryException {
        return getAuthorizable(User.class, id);
    }

    public Group getGroup(String id) throws RepositoryException {
        return getAuthorizable(Group.class, id);
    }
}
