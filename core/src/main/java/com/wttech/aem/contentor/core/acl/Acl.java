package com.wttech.aem.contentor.core.acl;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlManager;
import java.util.Collection;

public class Acl {

    private final ResourceResolver resourceResolver;

    public Acl(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    public User createUser(String id) throws AclException {
        return createUser(UserOptions.simple(id));
    }

    public User createUser(UserOptions options) throws AclException {
        throw new IllegalStateException("Not implemented yet!");
    }

    void removeUser(User user) throws AclException {
        throw new IllegalStateException("Not implemented yet!");
    }

    public User createSystemUser() throws AclException {
        throw new IllegalStateException("Not implemented yet!");
    }

    public Group createGroup() throws AclException {
        throw new IllegalStateException("Not implemented yet!");
    }

    void removeGroup(Group group) throws AclException {
        throw new IllegalStateException("Not implemented yet!");
    }

    public void purge(Authorizable authorizable, String path) throws AclException {
        throw new IllegalStateException("Not implemented yet!");
    }

    public void allow(Authorizable authorizable, String path, Collection<String> permissions) throws AclException {
        allow(authorizable, AllowOptions.simple(path, permissions));
    }

    public void allow(Authorizable authorizable, AllowOptions options) throws AclException {
        throw new IllegalStateException("Not implemented yet!");
    }

    public void deny(Authorizable authorizable, String path, Collection<String> permissions) throws AclException {
        deny(authorizable, DenyOptions.simple(path, permissions));
    }

    public void deny(Authorizable authorizable, DenyOptions options) throws AclException {
        throw new IllegalStateException("Not implemented yet!");
    }

    private ValueFactory getValueFactory() throws RepositoryException {
        return resourceResolver.adaptTo(Session.class).getValueFactory();
    }

    private AccessControlManager getAccessControlManager() throws RepositoryException {
        return resourceResolver.adaptTo(Session.class).getAccessControlManager();
    }
}
