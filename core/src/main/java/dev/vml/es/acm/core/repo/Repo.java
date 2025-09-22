package dev.vml.es.acm.core.repo;

import dev.vml.es.acm.core.util.StreamUtils;
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

    private final Locker locker;

    private boolean autoCommit = true;

    public Repo(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
        this.session = resourceResolver.adaptTo(Session.class);
        this.locker = new Locker(resourceResolver, this::isAutoCommit);
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

    public void autoCommit(boolean enabled) {
        if (this.autoCommit != enabled) {
            if (enabled) {
                LOG.info("Auto-commit is now enabled. Changes will be committed after each operation.");
            } else {
                LOG.info("Auto-commit is now disabled. Changes will not be committed after each operation.");
            }
            this.autoCommit = enabled;
        }
    }

    public void commit() {
        try {
            LOG.debug("Committing manually changes to repository.");
            resourceResolver.commit();
        } catch (PersistenceException e) {
            throw new RepoException("Cannot manually commit changes to repository!", e);
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

    public void revert() {
        try {
            resourceResolver.revert();
        } catch (Exception e) {
            throw new RepoException("Cannot revert changes in repository!", e);
        }
    }

    public void dryRun(boolean enabled, Runnable operation) {
        boolean autoCommitInitial = this.autoCommit;
        try {
            if (enabled) {
                LOG.info("Dry run is enabled. Changes will not be committed to the repository.");
                if (autoCommitInitial) {
                    this.autoCommit = false;
                }
                operation.run();
            } else {
                LOG.info("Dry run is disabled. Changes will be commited to the repository.");
                operation.run();
            }
        } finally {
            if (enabled) {
                if (autoCommitInitial) {
                    this.autoCommit = true;
                }
                revert();
                LOG.info("Dry run completed. Changes reverted.");
            }
        }
    }

    public Stream<RepoResource> query(String path) {
        return query(path, JcrConstants.NT_BASE, null, null);
    }

    public Stream<RepoResource> query(String path, String nodeType) {
        return query(path, nodeType, null, null);
    }

    public Stream<RepoResource> query(String path, String nodeType, String where) {
        return query(path, nodeType, where, null);
    }

    public Stream<RepoResource> query(String path, String nodeType, String where, String orderBy) {
        String sql = String.format("SELECT * FROM [%s] AS n WHERE ISDESCENDANTNODE(n, [%s])", nodeType, path);
        if (StringUtils.isNotBlank(where)) {
            sql += " AND (" + where + ")";
        }
        if (StringUtils.isNotBlank(orderBy)) {
            sql += " ORDER BY " + orderBy;
        }
        return queryRaw(sql);
    }

    public Stream<RepoResource> queryRaw(String sql) {
        return StreamUtils.asStream(resourceResolver.findResources(sql, Query.JCR_SQL2))
                .map(r -> new RepoResource(this, r.getPath()));
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

    public Locker getLocker() {
        return locker;
    }
}
