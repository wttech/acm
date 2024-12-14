package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.util.GroovyUtils;
import groovy.lang.Closure;
import java.util.Collection;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Acl {

  private static final Logger LOG = LoggerFactory.getLogger(Acl.class);

  private final ResourceResolver resourceResolver;

  public Acl(ResourceResolver resourceResolver) {
    this.resourceResolver = resourceResolver;
  }

  // TODO Closure accepting methods need to be defined before the simple ones (add arch unit rule to
  // protect it)
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

  // Non-closure accepting methods

  public User createUser(String id) throws AclException {
    CreateUserOptions options = new CreateUserOptions();
    options.setId(id);
    return createUser(options);
  }

  public User createUser(CreateUserOptions options) throws AclException {
    LOG.error("Not implemented yet! Creating user with options: {}", options);
    return null;
  }

  public User getUser(String id) throws AclException {
    LOG.error("Not implemented yet! Getting user with id: {}", id);
    return null;
  }

  public Group getGroup(String id) throws AclException {
    LOG.error("Not implemented yet! Getting group with id: {}", id);
    return null;
  }

  void removeUser(String id) throws AclException {
    removeUser(getUser(id));
  }

  void removeUser(User user) throws AclException {
    LOG.error("Not implemented yet! Removing user: {}", user);
  }

  public Group createGroup(String id) throws AclException {
    CreateGroupOptions options = new CreateGroupOptions();
    options.setId(id);
    return createGroup(options);
  }

  public Group createGroup(CreateGroupOptions options) throws AclException {
    LOG.error("Not implemented yet! Creating group with options: {}", options);
    return null;
  }

  void removeGroup(String id) throws AclException {
    removeGroup(getGroup(id));
  }

  void removeGroup(Group group) throws AclException {
    LOG.error("Not implemented yet! Removing group: {}", group);
  }

  public void purge(Authorizable authorizable, String path) throws AclException {
    LOG.error("Not implemented yet! Purging authorizable: {} at path: {}", authorizable, path);
  }

  public void allow(Authorizable authorizable, String path, Collection<String> permissions)
      throws AclException {
    AllowOptions options = new AllowOptions();
    options.setAuthorizable(authorizable);
    options.setPath(path);
    options.setPermissions(permissions);
    allow(options);
  }

  public void allow(AllowOptions options) throws AclException {
    LOG.error("Not implemented yet! Allowing options: {}", options);
  }

  public void deny(Authorizable authorizable, String path, Collection<String> permissions)
      throws AclException {
    DenyOptions options = new DenyOptions();
    options.setAuthorizable(authorizable);
    options.setPath(path);
    options.setPermissions(permissions);
    deny(options);
  }

  public void deny(DenyOptions options) throws AclException {
    LOG.error("Not implemented yet! Denying options: {}", options);
  }

  private ValueFactory getValueFactory() throws RepositoryException {
    return resourceResolver.adaptTo(Session.class).getValueFactory();
  }

  private AccessControlManager getAccessControlManager() throws RepositoryException {
    return resourceResolver.adaptTo(Session.class).getAccessControlManager();
  }
}
