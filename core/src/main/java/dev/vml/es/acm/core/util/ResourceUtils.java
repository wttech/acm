package dev.vml.es.acm.core.util;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.AcmException;

import java.util.*;
import javax.jcr.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.resource.LoginException;

public final class ResourceUtils {

    public enum Subservice {
        CONTENT(AcmConstants.CODE + "-content-service"),
        MOCK(AcmConstants.CODE + "-mock-service");

        public final String userId;

        Subservice(String userId) {
            this.userId = userId;
        }

        public String id() {
            return name().toLowerCase();
        }
    }

    private ResourceUtils() {
        // intentionally empty
    }

    public static ResourceResolver contentResolver(
            ResourceResolverFactory resourceResolverFactory, String userImpersonationId) throws LoginException {
        return serviceResolver(resourceResolverFactory, Subservice.CONTENT, userImpersonationId);
    }

    public static ResourceResolver mockResolver(ResourceResolverFactory resourceResolverFactory) throws LoginException {
        return serviceResolver(resourceResolverFactory, Subservice.MOCK, null);
    }

    private static ResourceResolver serviceResolver(
            ResourceResolverFactory resourceResolverFactory, Subservice subservice, String userImpersonationId)
            throws LoginException {
        Map<String, Object> params = new HashMap<>();
        params.put(ResourceResolverFactory.SUBSERVICE, subservice.id());

        boolean impersonation = StringUtils.isNotBlank(userImpersonationId)
                && !StringUtils.equals(subservice.userId, userImpersonationId);
        if (!impersonation) {
            return resourceResolverFactory.getServiceResourceResolver(params);
        }

        try {
            params.put(ResourceResolverFactory.USER_IMPERSONATION, userImpersonationId);
            ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(params);
            String userImpersonationIdEffective = serviceOrImpersonatedUserId(resolver);
            if (!StringUtils.equals(userImpersonationId, userImpersonationIdEffective)) {
                throw new AcmException(String.format(
                        "Cannot impersonate user '%s' as service user '%s' is used instead!",
                        serviceOrImpersonatedUserId(resolver), userImpersonationId));
            }
            return resolver;
        } catch (LoginException e) {
            // fix for 'Impersonation not allowed' on 6.5.0 (supported by login admin whitelist)
            return resourceResolverFactory.getAdministrativeResourceResolver(params);
        }
    }

    /**
     * When user is impersonated, resolver's user ID remains the same as the one of the service user.
     * To get the effective user ID, leverage the session user who effectively performs the operations on the repository.
     */
    public static String serviceOrImpersonatedUserId(ResourceResolver resourceResolver) {
        Session session = resourceResolver.adaptTo(Session.class);
        if (session == null) {
            throw new AcmException("Cannot determine service/impersonated user ID from resource resolver!");
        }
        return session.getUserID();
    }

    /**
     * Move in-place resource from source path to target path.
     * Resolver is not doing it in-place, but JCR workspace is.
     */
    public static void move(ResourceResolver resolver, String sourcePath, String targetPath)
            throws PersistenceException {
        Workspace workspace = Optional.ofNullable(resolver)
                .map(r -> r.adaptTo(Session.class))
                .map(Session::getWorkspace)
                .orElse(null);
        if (workspace == null) {
            throw new PersistenceException(String.format(
                    "Cannot move resource from '%s' to '%s' as cannot access workspace!", sourcePath, targetPath));
        }
        try {
            workspace.move(sourcePath, targetPath);
        } catch (RepositoryException e) {
            throw new PersistenceException(
                    String.format(
                            "Cannot move resource from '%s' to '%s' due to workspace move error!",
                            sourcePath, targetPath),
                    e);
        }
    }

    /**
     * Creates a resource and its parents at the specified path if it does not exist.
     * Differs to {@link ResourceUtil#getOrCreateResource} in that it does not force putting 'sling:resourceType' property.
     */
    public static Resource ensure(ResourceResolver resourceResolver, String path, Map<String, Object> props, String primaryType, String parentPrimaryType) throws AcmException {
        try {
            Resource resource = resourceResolver.getResource(path);
            if (resource != null) {
                return resource;
            }

            String[] segments = StringUtils.strip(path, "/").split("/");
            StringBuilder currentPath = new StringBuilder();
            Resource parent = Objects.requireNonNull(resourceResolver.getResource("/"));

            for (int i = 0; i < segments.length; i++) {
                currentPath.append("/").append(segments[i]);
                Resource child = resourceResolver.getResource(currentPath.toString());
                if (child == null) {
                    Map<String, Object> allProps = new HashMap<>();
                    if (i == segments.length - 1) {
                        allProps.put(JcrConstants.JCR_PRIMARYTYPE, primaryType);
                        if (props != null) {
                            allProps.putAll(props);
                        }
                    } else {
                        allProps.put(JcrConstants.JCR_PRIMARYTYPE, parentPrimaryType);
                    }
                    child = resourceResolver.create(parent, segments[i], allProps);
                }
                parent = child;
            }
            return parent;
        } catch (PersistenceException e) {
            throw new AcmException(String.format("Cannot create resource '%s'!", path), e);
        }
    }

    public static Resource ensure(ResourceResolver resourceResolver, String path, String primaryType) throws AcmException {
        return ensure(resourceResolver, path, Collections.emptyMap(), primaryType, primaryType);
    }

    /**
     * Performs a deep, recursive copy of a resource from the specified source path to the target path.
     * <p>
     * This operation is executed in transient mode, meaning all changes remain in-memory and are not persisted
     * until an explicit commit is performed on the {@link ResourceResolver}. Unlike {@link ResourceResolver#copy(String, String)},
     * this method allows for dry-run scenarios and supports uncommitted changes, making it suitable for batch operations
     * and complex repository manipulations.
     */
    public static void copy(ResourceResolver resourceResolver, String sourcePath, String targetPath) {
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
            throw new AcmException(String.format("Cannot copy resource from '%s' to '%s'!", sourcePath, targetPath), e);
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
