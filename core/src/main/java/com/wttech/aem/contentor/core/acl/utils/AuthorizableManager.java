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

/**
 * The type Authorizable manager.
 */
public class AuthorizableManager {

    private final UserManager userManager;

    private final ValueFactory valueFactory;

    /**
     * Instantiates a new Authorizable manager.
     *
     * @param userManager  the user manager
     * @param valueFactory the value factory
     */
    public AuthorizableManager(UserManager userManager, ValueFactory valueFactory) {
        this.userManager = userManager;
        this.valueFactory = valueFactory;
    }

    /**
     * Create user user.
     *
     * @param id       the id
     * @param password the password
     * @param path     the path
     * @return the user
     * @throws RepositoryException the repository exception
     */
    public User createUser(String id, String password, String path) throws RepositoryException {
        Principal principal = () -> id;
        if (StringUtils.isEmpty(password)) {
            password = PasswordUtils.generateRandomPassword();
        }
        return userManager.createUser(
                id, StringUtils.defaultString(password, PasswordUtils.generateRandomPassword()), principal, path);
    }

    /**
     * Create group group.
     *
     * @param id         the id
     * @param path       the path
     * @param externalId the external id
     * @return the group
     * @throws RepositoryException the repository exception
     */
    public Group createGroup(String id, String path, String externalId) throws RepositoryException {
        Principal principal = () -> id;
        Group group = userManager.createGroup(id, principal, path);
        if (StringUtils.isNotEmpty(externalId)) {
            group.setProperty("rep:externalId", valueFactory.createValue(externalId));
        }
        return group;
    }

    /**
     * Create system user user.
     *
     * @param id   the id
     * @param path the path
     * @return the user
     * @throws RepositoryException the repository exception
     */
    public User createSystemUser(String id, String path) throws RepositoryException {
        return userManager.createSystemUser(id, path);
    }

    /**
     * Update authorizable.
     *
     * @param authorizable the authorizable
     * @param properties   the properties
     * @throws RepositoryException the repository exception
     */
    public void updateAuthorizable(Authorizable authorizable, Map<String, String> properties)
            throws RepositoryException {
        for (Map.Entry<String, String> property : properties.entrySet()) {
            authorizable.setProperty(property.getKey(), valueFactory.createValue(property.getValue()));
        }
    }

    /**
     * Update user.
     *
     * @param user       the user
     * @param password   the password
     * @param properties the properties
     * @throws RepositoryException the repository exception
     */
    public void updateUser(User user, String password, Map<String, String> properties) throws RepositoryException {
        if (StringUtils.isNotEmpty(password)) {
            user.changePassword(password);
        }
        updateAuthorizable(user, properties);
    }

    /**
     * Update group.
     *
     * @param group      the group
     * @param properties the properties
     * @throws RepositoryException the repository exception
     */
    public void updateGroup(Group group, Map<String, String> properties) throws RepositoryException {
        updateAuthorizable(group, properties);
    }

    /**
     * Delete user.
     *
     * @param user the user
     * @throws RepositoryException the repository exception
     */
    public void deleteUser(User user) throws RepositoryException {
        Iterator<Group> groups = user.memberOf();
        while (groups.hasNext()) {
            groups.next().removeMember(user);
        }
        user.remove();
    }

    /**
     * Delete group.
     *
     * @param group the group
     * @throws RepositoryException the repository exception
     */
    public void deleteGroup(Group group) throws RepositoryException {
        group.remove();
    }

    /**
     * Add to group acl result.
     *
     * @param authorizable the authorizable
     * @param group        the group
     * @return the acl result
     * @throws RepositoryException the repository exception
     */
    public AclResult addToGroup(Authorizable authorizable, Group group) throws RepositoryException {
        if (!group.isMember(authorizable)) {
            return group.addMember(authorizable) ? AclResult.OK : AclResult.FAILED;
        }
        return AclResult.SKIPPED;
    }

    /**
     * Remove from group acl result.
     *
     * @param authorizable the authorizable
     * @param group        the group
     * @return the acl result
     * @throws RepositoryException the repository exception
     */
    public AclResult removeFromGroup(Authorizable authorizable, Group group) throws RepositoryException {
        if (group.isMember(authorizable)) {
            return group.removeMember(authorizable) ? AclResult.OK : AclResult.FAILED;
        }
        return AclResult.SKIPPED;
    }

    /**
     * Gets authorizable.
     *
     * @param id the id
     * @return the authorizable
     * @throws RepositoryException the repository exception
     */
    public Authorizable getAuthorizable(String id) throws RepositoryException {
        return userManager.getAuthorizable(id);
    }

    /**
     * Gets authorizable.
     *
     * @param <T>               the type parameter
     * @param authorizableClass the authorizable class
     * @param id                the id
     * @return the authorizable
     * @throws RepositoryException the repository exception
     */
    public <T extends Authorizable> T getAuthorizable(Class<T> authorizableClass, String id)
            throws RepositoryException {
        Authorizable authorizable = getAuthorizable(id);
        if (authorizable != null && !authorizableClass.isInstance(authorizable)) {
            throw new AclException(
                    String.format("Authorizable with id %s exists but is a %s", id, authorizableClass.getSimpleName()));
        }
        return authorizableClass.cast(authorizable);
    }

    /**
     * Gets user.
     *
     * @param id the id
     * @return the user
     * @throws RepositoryException the repository exception
     */
    public User getUser(String id) throws RepositoryException {
        return getAuthorizable(User.class, id);
    }

    /**
     * Gets group.
     *
     * @param id the id
     * @return the group
     * @throws RepositoryException the repository exception
     */
    public Group getGroup(String id) throws RepositoryException {
        return getAuthorizable(Group.class, id);
    }

    /**
     * Sets property.
     *
     * @param authorizable the authorizable
     * @param name         the name
     * @param value        the value
     * @throws RepositoryException the repository exception
     */
    public void setProperty(Authorizable authorizable, String name, String value) throws RepositoryException {
        authorizable.setProperty(name, valueFactory.createValue(value));
    }

    /**
     * Remove property.
     *
     * @param authorizable the authorizable
     * @param name         the name
     * @throws RepositoryException the repository exception
     */
    public void removeProperty(Authorizable authorizable, String name) throws RepositoryException {
        authorizable.removeProperty(name);
    }
}
