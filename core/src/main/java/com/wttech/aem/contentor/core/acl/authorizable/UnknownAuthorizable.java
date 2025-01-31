package com.wttech.aem.contentor.core.acl.authorizable;

import java.security.Principal;
import java.util.Iterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;

public class UnknownAuthorizable implements Authorizable {

    private final String id;

    public UnknownAuthorizable(String id) {
        this.id = id;
    }

    @Override
    public String getID() throws RepositoryException {
        return id;
    }

    @Override
    public boolean isGroup() {
        return false;
    }

    @Override
    public Principal getPrincipal() throws RepositoryException {
        return null;
    }

    @Override
    public Iterator<Group> declaredMemberOf() throws RepositoryException {
        return null;
    }

    @Override
    public Iterator<Group> memberOf() throws RepositoryException {
        return null;
    }

    @Override
    public void remove() throws RepositoryException {}

    @Override
    public Iterator<String> getPropertyNames() throws RepositoryException {
        return null;
    }

    @Override
    public Iterator<String> getPropertyNames(String relPath) throws RepositoryException {
        return null;
    }

    @Override
    public boolean hasProperty(String relPath) throws RepositoryException {
        return false;
    }

    @Override
    public void setProperty(String relPath, Value value) throws RepositoryException {}

    @Override
    public void setProperty(String relPath, Value[] values) throws RepositoryException {}

    @Override
    public Value[] getProperty(String relPath) throws RepositoryException {
        return new Value[0];
    }

    @Override
    public boolean removeProperty(String relPath) throws RepositoryException {
        return false;
    }

    @Override
    public String getPath() throws UnsupportedRepositoryOperationException, RepositoryException {
        return null;
    }
}
