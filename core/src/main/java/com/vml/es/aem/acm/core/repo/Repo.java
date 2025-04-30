package com.vml.es.aem.acm.core.repo;

import com.vml.es.aem.acm.core.util.StreamUtils;
import java.util.stream.Stream;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Repo {

    private static final Logger LOG = LoggerFactory.getLogger(Repo.class);

    private final ResourceResolver resourceResolver;

    private final Session session;

    private boolean autoCommit = true;

    public Repo(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
        this.session = resourceResolver.adaptTo(Session.class);
    }

    public RepoResource get(String path) {
        return new RepoResource(this, path);
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    public void commit() {
        try {
            resourceResolver.commit();
        } catch (PersistenceException e) {
            throw new RepoException("Cannot manually commit changes to repository!");
        }
    }

    public void commit(String context) {
        try {
            if (autoCommit) {
                resourceResolver.commit();
                LOG.debug("Committed changes to repository while {}!", context);
            } else {
                LOG.debug("Skipped committing changes to repository while {}!", context);
            }
        } catch (PersistenceException e) {
            throw new RepoException(String.format("Cannot commit changes to repository while %s!", context), e);
        }
    }

    public Stream<RepoResource> query(String path) {
        return query(path, JcrConstants.NT_BASE, null, null);
    }

    public Stream<RepoResource> query(String path, String nodeType) {
        return query(path, nodeType, null, null);
    }

    public Stream<RepoResource> query(String path, String nodeType, String whereSpec) {
        return query(path, nodeType, whereSpec, null);
    }

    public Stream<RepoResource> query(String path, String nodeType, String whereSpec, String orderBySpec) {
        String sql = String.format("SELECT * FROM [%s] AS n WHERE ISDESCENDANTNODE(n, [%s])", nodeType, path);
        if (StringUtils.isNotBlank(whereSpec)) {
            sql += " AND " + whereSpec;
        }
        if (StringUtils.isNotBlank(orderBySpec)) {
            sql += " ORDER BY " + orderBySpec;
        }
        return queryRaw(sql);
    }

    public Stream<RepoResource> queryRaw(String sql) {
        return StreamUtils.asStream(resourceResolver.findResources(sql, Query.JCR_SQL2))
                .map(r -> new RepoResource(this, r.getPath()));
    }

    public Resource requireResource(String path) {
        Resource resource = resourceResolver.getResource(path);
        if (resource == null) {
            throw new RepoException(String.format("Resource at path '%s' does not exist!", path));
        }
        return resource;
    }

    public boolean isCompositeNodeStore() {
        try {
            Node node = session.getNode("/apps");
            boolean hasPermission = session.hasPermission("/", Session.ACTION_SET_PROPERTY);
            boolean hasCapability = session.hasCapability("addNode", node, new Object[] {"nt:folder"});
            return hasPermission && !hasCapability;
        } catch (Exception e) {
            throw new RepoException("Repository composite node store cannot be checked!", e);
        }
    }

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public Session getSession() {
        return session;
    }
}
