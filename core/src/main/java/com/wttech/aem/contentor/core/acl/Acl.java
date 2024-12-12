package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.utils.AuthorizableManager;
import com.wttech.aem.contentor.core.util.GroovyUtils;
import groovy.lang.Closure;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.resource.ResourceResolver;

public class Acl {

    private final Context context;

    public Acl(ResourceResolver resourceResolver, OutputStream out) throws AclException {
        this.context = new Context(resourceResolver, out);
    }

    // TODO Closure accepting methods need to be defined before the simple ones (add arch unit rule to protect it)
    public User createUser(Closure<CreateUserOptions> closure) throws AclException {
        return createUser(GroovyUtils.with(new CreateUserOptions(), closure));
    }

    public Group createGroup(Closure<CreateGroupOptions> closure) throws AclException {
        return createGroup(GroovyUtils.with(new CreateGroupOptions(), closure));
    }

    public void allow(Closure<AllowOptions> closure) throws AclException {
        allow(GroovyUtils.with(new AllowOptions(), closure));
    }

    public void deny(Closure<DenyOptions> closure) throws AclException {
        deny(GroovyUtils.with(new DenyOptions(), closure));
    }

    public void setProperty(Closure<PropertyOptions> closure) throws AclException {
        setProperty(GroovyUtils.with(new PropertyOptions(), closure));
    }

    // Non-closure accepting methods

    public User createUser(String id) throws AclException {
        CreateUserOptions options = new CreateUserOptions();
        options.setId(id);
        return createUser(options);
    }

    public User createUser(CreateUserOptions options) throws AclException {
        AuthorizableManager manager = context.getAuthorizableManager();
        User user = manager.getUser(options.getId());
        if (user == null) {
            user = manager.createUser(options.getId(), options.getPassword(), options.getPath());
        }
        manager.updateUser(user, options.getPassword(), options.determineProperties());
        return user;
    }

    public User getUser(String id) throws AclException {
        return context.getAuthorizableManager().getUser(id);
    }

    public Group getGroup(String id) throws AclException {
        return context.getAuthorizableManager().getGroup(id);
    }

    void removeUser(String id) throws AclException {
        removeUser(getUser(id));
    }

    void removeUser(User user) throws AclException {
        context.getAuthorizableManager().removeUser(user);
    }

    public User createSystemUser() throws AclException {
        throw new IllegalStateException("Not implemented yet!");
    }

    public Group createGroup(String id) throws AclException {
        CreateGroupOptions options = new CreateGroupOptions();
        options.setId(id);
        return createGroup(options);
    }

    public Group createGroup(CreateGroupOptions options) throws AclException {
        AuthorizableManager manager = context.getAuthorizableManager();
        Group group = getGroup(options.getId());
        if (group == null) {
            group = manager.createGroup(options.getId(), options.getPath(), options.getExternalId());
        }
        manager.updateGroup(group, options.determineProperties());
        return group;
    }

    void removeGroup(String id) throws AclException {
        removeGroup(getGroup(id));
    }

    void removeGroup(Group group) throws AclException {
        context.getAuthorizableManager().removeGroup(group);
    }

    public void purge(Authorizable authorizable, String path) throws AclException {
        throw new IllegalStateException("Not implemented yet!");
    }

    public void allow(Authorizable authorizable, String path, Collection<String> permissions) throws AclException {
        AllowOptions options = new AllowOptions();
        options.setAuthorizable(authorizable);
        options.setPath(path);
        options.setPermissions(permissions);
        allow(options);
    }

    public void allow(AllowOptions options) throws AclException {
        throw new IllegalStateException("Not implemented yet!");
    }

    public void deny(Authorizable authorizable, String path, Collection<String> permissions) throws AclException {
        DenyOptions options = new DenyOptions();
        options.setAuthorizable(authorizable);
        options.setPath(path);
        options.setPermissions(permissions);
        deny(options);
    }

    public void deny(DenyOptions options) throws AclException {
        throw new IllegalStateException("Not implemented yet!");
    }

    public void setProperty(PropertyOptions options) throws AclException {
        Authorizable authorizable = options.getAuthorizable();
        if (authorizable == null) {
            authorizable = context.getAuthorizableManager().getAuthorizable(options.getId());
        }
        setProperty(authorizable, options.getKey(), options.getValue());
    }

    public void setProperty(String id, String key, String value) {
        Authorizable authorizable = context.getAuthorizableManager().getAuthorizable(id);
        setProperty(authorizable, key, value);
    }

    public void setProperty(Authorizable authorizable, String key, String value) {
        Map<String, String> properties = Collections.singletonMap(key, value);
        context.getAuthorizableManager().updateAuthorizable(authorizable, properties);
    }

    public void save() {
        try {
            context.getSession().save();
        } catch (RepositoryException e) {
            throw new AclException(e);
        }
    }
}
