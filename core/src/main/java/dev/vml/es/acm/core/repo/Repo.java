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
import org.slf4j.helpers.NOPLogger;

public class Repo {

    private static final Logger LOG = LoggerFactory.getLogger(Repo.class);

    private final ResourceResolver resourceResolver;

    private final Session session;

    private final Locker locker;

    private boolean autoCommit = true;

    private boolean quiet = false;

    public Repo(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
        this.session = resourceResolver.adaptTo(Session.class);
        this.locker = new Locker(resourceResolver, this::isAutoCommit);
    }

    public static Repo quiet(ResourceResolver resourceResolver) {
        Repo repo = new Repo(resourceResolver);
        repo.setQuiet(true);
        return repo;
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

    public boolean isQuiet() {
        return quiet;
    }

    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    public Logger getLogger() {
        return quiet ? NOPLogger.NOP_LOGGER : LOG;
    }

    public void autoCommit(boolean enabled) {
        if (this.autoCommit != enabled) {
            if (enabled) {
                getLogger().info("Auto-commit is now enabled. Changes will be committed after each operation.");
            } else {
                getLogger().info("Auto-commit is now disabled. Changes will not be committed after each operation.");
            }
            this.autoCommit = enabled;
        }
    }

    public void commit() {
        try {
            getLogger().debug("Committing manually changes to repository.");
            resourceResolver.commit();
        } catch (PersistenceException e) {
            throw new RepoException("Cannot manually commit changes to repository!", e);
        }
    }

    public void commit(String context) {
        try {
            if (autoCommit) {
                resourceResolver.commit();
                getLogger().debug("Committed changes to repository while {}!", context);
            } else {
                getLogger().debug("Skipped committing changes to repository while {}!", context);
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
                getLogger().info("Dry run is enabled. Changes will not be committed to the repository.");
                if (autoCommitInitial) {
                    this.autoCommit = false;
                }
                operation.run();
            } else {
                getLogger().info("Dry run is disabled. Changes will be commited to the repository.");
                operation.run();
            }
        } finally {
            if (enabled) {
                if (autoCommitInitial) {
                    this.autoCommit = true;
                }
                revert();
                getLogger().info("Dry run completed. Changes reverted.");
            }
        }
    }

    public void quiet(Runnable operation) {
        quiet(true, operation);
    }

    public void quiet(boolean enabled, Runnable operation) {
        boolean quietInitial = this.quiet;
        try {
            if (enabled) {
                this.quiet = true;
                operation.run();
            } else {
                operation.run();
            }
        } finally {
            this.quiet = quietInitial;
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
