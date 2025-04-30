package com.vml.es.aem.acm.core.repo;

import com.vml.es.aem.acm.core.util.ResourceSpliterator;
import com.vml.es.aem.acm.core.util.StreamUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
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

    public Resource ensureFolder(String path) {
        return ensure(path, JcrResourceConstants.NT_SLING_FOLDER);
    }

    public Resource ensureOrderedFolder(String path) {
        return ensure(path, JcrResourceConstants.NT_SLING_ORDERED_FOLDER);
    }

    public Resource ensure(String path, String resourceType) {
        Resource resource = resourceResolver.getResource(path);
        if (resource == null) {
            try {
                ResourceUtil.getOrCreateResource(resourceResolver, path, resourceType, resourceType, false);
            } catch (PersistenceException e) {
                throw new RepoException(String.format("Cannot ensure '%s' at path '%s'!", resourceType, path), e);
            }
            commit(String.format("ensuring '%s' at path %s", resourceType, path));
            LOG.info("Ensured '{}' at path '{}'", resourceType, path);
        } else {
            LOG.info("Skipped ensuring '{}' at path '{}'", resourceType, path);
        }

        return resource;
    }

    public Resource save(String path, Map<String, Object> values) {
        Resource result = resourceResolver.getResource(path);
        if (result == null) {
            String parentPath = StringUtils.substringBeforeLast(path, "/");
            Resource parent = resourceResolver.getResource(parentPath);
            if (parent == null) {
                throw new RepoException(
                        String.format("Cannot save resource as parent path '%s' does not exist!", parentPath));
            }
            try {
                String name = StringUtils.substringAfterLast(path, "/");
                result = resourceResolver.create(parent, name, values);
                commit(String.format("creating resource at path '%s'", path));
                LOG.info("Created resource at path '{}'", path);
                return result;
            } catch (PersistenceException e) {
                throw new RepoException(String.format("Cannot save resource at path '%s'!", path), e);
            }
        } else {
            ModifiableValueMap valueMap = Objects.requireNonNull(result.adaptTo(ModifiableValueMap.class));
            valueMap.putAll(values);
            commit(String.format("updating resource at path '%s'", path));
            LOG.info("Updated resource at path '{}'", path);
        }
        return result;
    }

    public void saveProp(String path, String key, Object value) {
        Resource resource = resourceResolver.getResource(path);
        if (resource == null) {
            throw new RepoException(
                    String.format("Cannot save property '%s' at path '%s' as it does not exist!", key, path));
        }
        ModifiableValueMap props = Objects.requireNonNull(resource.adaptTo(ModifiableValueMap.class));
        Object valueExisting = props.get(key);
        if (Objects.equals(value, valueExisting)) {
            LOG.info(
                    "Skipped saving property '{}' at path '{}' as it already exists with the same value '{}'!",
                    key,
                    path,
                    value);
            return;
        }
        props.put(key, value);

        if (valueExisting == null) {
            LOG.info("Created property '{}' at path '{}' with value '{}'", key, path, value);
            commit(String.format("creating property '%s' at path '%s'", key, path));
        } else {
            LOG.info("Updated property '{}' at path '{}' from value '{}' to '{}'", key, path, valueExisting, value);
            commit(String.format("updating property '%s' at path '%s'", key, path));
        }
    }

    public boolean delete(String path) {
        Resource resource = resourceResolver.getResource(path);
        if (resource == null) {
            LOG.info("Skipped deletion as resource does not exist at path '{}'", path);
            return false;
        }
        try {
            resourceResolver.delete(resource);
        } catch (PersistenceException e) {
            throw new RepoException(String.format("Cannot delete resource at path '%s'!", path), e);
        }
        LOG.info("Deleted resource at path '{}'", path);
        return true;
    }

    public void commit() {
        try {
            resourceResolver.commit();
        } catch (PersistenceException e) {
            throw new RepoException("Cannot manually commit changes to repository!");
        }
    }

    private void commit(String context) {
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

    public boolean exists(String path) {
        try {
            return session.nodeExists(path);
        } catch (Exception e) {
            throw new RepoException(String.format("Repository path '%s' cannot be checked for existence!", path), e);
        }
    }

    public RepoResource get(String path) {
        return new RepoResource(this, path);
    }

    public Resource require(String path) {
        Resource resource = resourceResolver.getResource(path);
        if (resource == null) {
            throw new RepoException(String.format("Resource at path '%s' does not exist!", path));
        }
        return resource;
    }

    public Resource saveFile(String path, Object data, String mimeType) {
        Resource mainResource = resourceResolver.getResource(path);
        try {
            if (mainResource == null) {
                String parentPath = StringUtils.substringBeforeLast(path, "/");
                Resource parent = resourceResolver.getResource(parentPath);
                if (parent == null) {
                    throw new RepoException(
                            String.format("Cannot save file as parent path '%s' does not exist!", parentPath));
                }
                String name = StringUtils.substringAfterLast(path, "/");
                Map<String, Object> mainValues = new HashMap<>();
                mainValues.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FILE);
                mainResource = resourceResolver.create(parent, name, mainValues);

                Map<String, Object> contentValues = new HashMap<>();
                setFileContent(contentValues, data, mimeType);
                resourceResolver.create(mainResource, JcrConstants.JCR_CONTENT, contentValues);

                commit(String.format("creating file at path '%s'", path));
                LOG.info("Created file at path '{}'", path);
            } else {
                Resource contentResource = mainResource.getChild(JcrConstants.JCR_CONTENT);
                if (contentResource == null) {
                    Map<String, Object> contentValues = new HashMap<>();
                    setFileContent(contentValues, data, mimeType);
                    resourceResolver.create(mainResource, JcrConstants.JCR_CONTENT, contentValues);
                } else {
                    ModifiableValueMap contentValues =
                            Objects.requireNonNull(contentResource.adaptTo(ModifiableValueMap.class));
                    setFileContent(contentValues, data, mimeType);
                }

                commit(String.format("updating file at path '%s'", path));
                LOG.info("Updated file at path '{}'", path);
            }
        } catch (PersistenceException e) {
            throw new RepoException(String.format("Cannot save file at path '%s'!", path), e);
        }
        return mainResource;
    }

    private void setFileContent(Map<String, Object> contentValues, Object data, String mimeType) {
        contentValues.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_RESOURCE);
        contentValues.put(JcrConstants.JCR_ENCODING, "utf-8");
        contentValues.put(JcrConstants.JCR_DATA, data);
        contentValues.put(JcrConstants.JCR_MIMETYPE, mimeType);
    }

    public InputStream readFileAsStream(String path) {
        Resource resource = require(path);
        Resource contentResource = resource.getChild(JcrConstants.JCR_CONTENT);
        if (contentResource == null) {
            throw new RepoException(String.format("Cannot read file at path '%s' as it does not have content!", path));
        }
        InputStream inputStream = contentResource.adaptTo(InputStream.class);
        if (inputStream == null) {
            throw new RepoException(
                    String.format("Cannot read file at path '%s' as it does not have content stream!", path));
        }
        return inputStream;
    }

    public String readFileAsString(String path) {
        try {
            return IOUtils.toString(readFileAsStream(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RepoException(String.format("Cannot read file as path '%s' as string!", path), e);
        }
    }

    public boolean isFile(String path) {
        return isType(path, JcrConstants.NT_FILE);
    }

    public boolean isType(String path, String resourceType) {
        return Optional.ofNullable(resourceResolver.getResource(path))
                .map(r -> r.isResourceType(resourceType))
                .orElse(false);
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
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

    public Stream<RepoResource> children(String path) {
        return StreamUtils.asStream(require(path).listChildren()).map(r -> new RepoResource(this, r.getPath()));
    }

    public Stream<RepoResource> traverse(String path) {
        return traverse(path, true);
    }

    public Stream<RepoResource> traverse(String path, boolean includeSelf) {
        Stream<RepoResource> result =
                ResourceSpliterator.stream(require(path)).map(r -> new RepoResource(this, r.getPath()));
        if (!includeSelf) {
            result = result.skip(1);
        }
        return result;
    }

    public RepoState state(String path) {
        Resource resource = resourceResolver.getResource(path);
        if (resource == null) {
            return new RepoState(path, false, new ValueMapDecorator(Collections.emptyMap()));
        }
        return new RepoState(path, true, resource.getValueMap());
    }
}
