package com.vml.es.aem.acm.core.util;

import com.vml.es.aem.acm.core.AcmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.*;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;

public final class ResourceUtils {

    public enum Subservice {
        CONTENT("acm-content-service"),
        MOCK("acm-mock-service");

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

        boolean impersonation = StringUtils.isNotBlank(userImpersonationId);
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

    public static Resource makeFolders(ResourceResolver resourceResolver, String path) throws AcmException {
        try {
            return ResourceUtil.getOrCreateResource(
                    resourceResolver,
                    path,
                    JcrResourceConstants.NT_SLING_FOLDER,
                    JcrResourceConstants.NT_SLING_FOLDER,
                    true);
        } catch (Exception e) {
            throw new AcmException(String.format("Folders cannot be created for path '%s'", path), e);
        }
    }
}
