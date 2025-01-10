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

    public void updateAuthorizable(Authorizable authorizable, Map<String, String> properties) {
        try {
            for (Map.Entry<String, String> property : properties.entrySet()) {
                authorizable.setProperty(property.getKey(), valueFactory.createValue(property.getValue()));
            }
        } catch (RepositoryException e) {
            throw new AclException("Failed to update authorizable", e);
        }
    }

    public void updateUser(User user, String password, Map<String, String> properties) {
        try {
            if (StringUtils.isNotEmpty(password)) {
                user.changePassword(password);
            }
            updateAuthorizable(user, properties);
        } catch (RepositoryException e) {
            throw new AclException("Failed to update user", e);
        }
    }

    public void updateGroup(Group group, Map<String, String> properties) {
        updateAuthorizable(group, properties);
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

    public AclResult addToGroup(Authorizable authorizable, Group group) {
        try {
            if (!group.isMember(authorizable)) {
                return group.addMember(authorizable) ? AclResult.OK : AclResult.FAILED;
            }
            return AclResult.SKIPPED;
        } catch (RepositoryException e) {
            throw new AclException("Failed to add authorizable to group", e);
        }
    }

    public AclResult removeFromGroup(Authorizable authorizable, Group group) {
        try {
            if (group.isMember(authorizable)) {
                return group.removeMember(authorizable) ? AclResult.OK : AclResult.FAILED;
            }
        } catch (RepositoryException e) {
            throw new AclException("Failed to remove authorizable from group", e);
        }
        return AclResult.SKIPPED;
    }

    public void removeFromAllGroups(Authorizable authorizable) {
        try {
            Iterator<Group> groups = authorizable.memberOf();
            while (groups.hasNext()) {
                Group group = groups.next();
                removeFromGroup(authorizable, group);
            }
        } catch (RepositoryException e) {
            throw new AclException("Failed to remove authorizable from all groups", e);
        }
    }

    public void removeMember(Group group, Authorizable member) {
        try {
            if (group.isMember(member)) {
                group.removeMember(member);
            }
        } catch (RepositoryException e) {
            throw new AclException("Failed to remove member from group", e);
        }
    }

    public void removeAllMembers(Group group) {
        try {
            Iterator<Authorizable> members = group.getMembers();
            while (members.hasNext()) {
                Authorizable member = members.next();
                removeMember(group, member);
            }
        } catch (RepositoryException e) {
            throw new AclException("Failed to remove all members from group", e);
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

    public void removeProperty(Authorizable authorizable, String name) {
        try {
            authorizable.removeProperty(name);
        } catch (RepositoryException e) {
            throw new AclException("Failed to remove property", e);
        }
    }
}
