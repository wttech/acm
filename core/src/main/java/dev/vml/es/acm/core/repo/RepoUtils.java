package dev.vml.es.acm.core.repo;

import java.util.*;
import javax.jcr.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.*;

public final class RepoUtils {

    private RepoUtils() {
        // intentionally empty
    }

    /**
     * Move in-place resource from source path to a target path.
     * Resolver is not doing it in-place, but JCR workspace is.
     */
    public static void move(ResourceResolver resolver, String sourcePath, String targetPath)
            throws RepoException {
        Workspace workspace = Optional.ofNullable(resolver)
                .map(r -> r.adaptTo(Session.class))
                .map(Session::getWorkspace)
                .orElse(null);
        if (workspace == null) {
            throw new RepoException(String.format(
                    "Cannot move resource from '%s' to '%s' as cannot access workspace!", sourcePath, targetPath));
        }
        try {
            workspace.move(sourcePath, targetPath);
        } catch (RepositoryException e) {
            throw new RepoException(
                    String.format(
                            "Cannot move resource from '%s' to '%s' due to workspace move error!",
                            sourcePath, targetPath),
                    e);
        }
    }

    /**
     * Creates a resource and its parents at the specified path if it does not exist.
     * Mostly copied, but differs to {@link ResourceUtil#getOrCreateResource} in that it does not force putting 'sling:resourceType' property.
     */
    public static Resource ensure(ResourceResolver resolver, String path, String resourceType, boolean autoCommit)
            throws RepoException {
        return ensure(resolver, path, resourceType, resourceType, autoCommit);
    }

    public static Resource ensure(
            ResourceResolver resolver,
            String path,
            String resourceType,
            String intermediateResourceType,
            boolean autoCommit)
            throws RepoException {
        final Map<String, Object> props;
        if (resourceType == null) {
            props = null;
        } else {
            props = Collections.emptyMap();
        }
        return ensure(resolver, path, props, intermediateResourceType, autoCommit);
    }

    public static Resource ensure(
            ResourceResolver resolver,
            String path,
            Map<String, Object> resourceProperties,
            String intermediateResourceType,
            boolean autoCommit)
            throws RepoException {
        PersistenceException exceptionLast = null;
        for (int i = 0; i < 5; i++) {
            try {
                return ensureInternal(resolver, path, resourceProperties, intermediateResourceType, autoCommit);
            } catch (PersistenceException pe) {
                if (autoCommit) {
                    // in case of exception, revert to last clean state and retry
                    resolver.revert();
                    resolver.refresh();
                    exceptionLast = pe;
                } else {
                    throw new RepoException(String.format("Cannot ensure resource at path '%s'!", path), exceptionLast);
                }
            }
        }
        throw new RepoException(String.format("Cannot ensure resource at path '%s'!", path), exceptionLast);
    }

    private static Resource ensureInternal(
            ResourceResolver resolver,
            String path,
            Map<String, Object> resourceProperties,
            String intermediateResourceType,
            boolean autoCommit)
            throws PersistenceException {
        Resource rsrc = resolver.getResource(path);
        if (rsrc == null) {
            int lastPos = path.lastIndexOf('/');
            String name = path.substring(lastPos + 1);

            Resource parentResource;
            if (lastPos == 0) {
                parentResource = resolver.getResource("/");
            } else {
                String parentPath = path.substring(0, lastPos);
                parentResource =
                        ensure(resolver, parentPath, intermediateResourceType, intermediateResourceType, autoCommit);
            }
            if (autoCommit) {
                resolver.refresh();
            }
            try {
                int retry = 5;
                while (retry > 0 && rsrc == null) {
                    rsrc = resolver.create(parentResource, name, resourceProperties);
                    // check for SNS
                    if (!name.equals(rsrc.getName())) {
                        resolver.refresh();
                        resolver.delete(rsrc);
                        rsrc = resolver.getResource(parentResource, name);
                    }
                    retry--;
                }
                if (rsrc == null) {
                    throw new PersistenceException(String.format("Cannot create resource at path '%s'!", path));
                }
            } catch (PersistenceException pe) {
                // this could be thrown because someone else tried to create this node concurrently
                resolver.refresh();
                rsrc = resolver.getResource(parentResource, name);
                if (rsrc == null) {
                    throw pe;
                }
            }
            if (autoCommit) {
                try {
                    resolver.commit();
                    resolver.refresh();
                    rsrc = resolver.getResource(parentResource, name);
                } catch (PersistenceException pe) {
                    // try again - maybe someone else did create the resource in the meantime, or we ran into
                    // Jackrabbit's stale item exception in a clustered environment
                    resolver.revert();
                    resolver.refresh();
                    rsrc = resolver.getResource(parentResource, name);
                    if (rsrc == null) {
                        rsrc = resolver.create(parentResource, name, resourceProperties);
                        resolver.commit();
                    }
                }
            }
        }
        return rsrc;
    }

    /**
     * Performs a deep, recursive copy of a resource from the specified source path to the target path.
     * <p>
     * This operation is executed in transient mode, meaning all changes remain in-memory and are not persisted
     * until an explicit commit is performed on the {@link ResourceResolver}. Unlike {@link ResourceResolver#copy(String, String)},
     * this method allows for dry-run scenarios and supports uncommitted changes, making it suitable for batch operations
     * and complex repository manipulations.
     */
    public static void copy(ResourceResolver resourceResolver, String sourcePath, String targetPath)
            throws RepoException {
        Session session = resourceResolver.adaptTo(Session.class);
        if (session == null) {
            throw new IllegalStateException("Cannot copy resource as session is not provided!");
        }
        try {
            Node sourceNode = session.getNode(sourcePath);
            String parentTargetPath = StringUtils.substringBeforeLast(targetPath, "/");
            String targetName = StringUtils.substringAfterLast(targetPath, "/");
            Node targetParent = session.getNode(parentTargetPath);

            copyInternal(sourceNode, targetParent, targetName);
        } catch (RepositoryException e) {
            throw new RepoException(
                    String.format("Cannot copy resource from '%s' to '%s'!", sourcePath, targetPath), e);
        }
    }

    private static void copyInternal(Node sourceNode, Node targetParent, String targetName) throws RepositoryException {
        Node targetNode =
                targetParent.addNode(targetName, sourceNode.getPrimaryNodeType().getName());

        // Copy properties
        PropertyIterator properties = sourceNode.getProperties();
        while (properties.hasNext()) {
            Property property = properties.nextProperty();
            if (!property.getDefinition().isProtected()) {
                if (property.isMultiple()) {
                    targetNode.setProperty(property.getName(), property.getValues());
                } else {
                    targetNode.setProperty(property.getName(), property.getValue());
                }
            }
        }

        // Recursively copy child nodes
        NodeIterator children = sourceNode.getNodes();
        while (children.hasNext()) {
            Node child = children.nextNode();
            copyInternal(child, targetNode, child.getName());
        }
    }
}
