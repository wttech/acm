package com.wttech.aem.acm.core.repository;

import javax.jcr.Node;
import javax.jcr.Session;
import org.apache.sling.api.resource.ResourceResolver;

// TODO probably 2 API are needed; Sling-based (for abstracted reading) and JCR-based (for writing)
public class Repository {

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

    public boolean exists(String path) {
        try {
            return session.nodeExists(path);
        } catch (Exception e) {
            throw new RepositoryException(
                    String.format("Repository path '%s' cannot be checked for existence!", path), e);
        }
    }
}
