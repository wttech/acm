package com.vml.es.aem.acm.core.repo;

import com.vml.es.aem.acm.core.util.ResourceUtils;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.Session;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Repository {

    private static final Logger LOG = LoggerFactory.getLogger(Repository.class);

    private final ResourceResolver resourceResolver;

    private final Session session;

    public Repository(ResourceResolver resourceResolver) {
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
            throw new RepositoryException("Repository composite node store cannot be checked!", e);
        }
    }

    public Resource makeFolders(String path) {
        Resource resource = resourceResolver.getResource(path);
        if (resource == null) {
            resource = ResourceUtils.makeFolders(resourceResolver, path);
            commit(String.format("creating folders at path %s", path));
            LOG.info("Created folders at path '{}'", path);
        } else {
            LOG.info("Skipped creation of folders at path '{}'", path);
        }

        return resource;
    }

    public Resource save(String path, Map<String, Object> values) {
        Resource result = resourceResolver.getResource(path);
        if (result == null) {
            String parentPath = StringUtils.substringBeforeLast(path, "/");
            Resource parent = resourceResolver.getResource(parentPath);
            if (parent == null) {
                throw new RepositoryException(
                        String.format("Cannot save resource as parent path '%s' does not exist!", parentPath));
            }
            try {
                String name = StringUtils.substringAfterLast(path, "/");
                result = resourceResolver.create(parent, name, values);
                commit(String.format("creating resource at path '%s'", path));
                LOG.info("Created resource at path '{}'", path);
                return result;
            } catch (PersistenceException e) {
                throw new RepositoryException(String.format("Cannot save resource at path '%s'!", path), e);
            }
        } else {
            ModifiableValueMap valueMap = result.adaptTo(ModifiableValueMap.class);
            valueMap.putAll(values);
            commit(String.format("updating resource at path '%s'", path));
            LOG.info("Updated resource at path '{}'", path);
        }
        return result;
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
            throw new RepositoryException(String.format("Cannot delete resource at path '%s'!", path), e);
        }
        LOG.info("Deleted resource at path '{}'", path);
        return true;
    }

    public void commit(String context) {
        try {
            resourceResolver.commit();
        } catch (PersistenceException e) {
            throw new RepositoryException(String.format("Cannot commit changes to repository while %s!", context), e);
        }
    }

    public boolean exists(String path) {
        try {
            return session.nodeExists(path);
        } catch (Exception e) {
            throw new RepositoryException(
                    String.format("Repository path '%s' cannot be checked for existence!", path), e);
        }
    }
}
