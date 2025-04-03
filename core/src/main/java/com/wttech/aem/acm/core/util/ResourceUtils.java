package com.wttech.aem.acm.core.util;

import com.wttech.aem.acm.core.AcmException;
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

    private ResourceUtils() {
        // intentionally empty
    }

    public static ResourceResolver serviceResolver(
            ResourceResolverFactory resourceResolverFactory, String userImpersonationId) throws LoginException {
        Map<String, Object> params = new HashMap<>();
        params.put(ResourceResolverFactory.SUBSERVICE, "acm");
        if (StringUtils.isNotBlank(userImpersonationId)) {
            params.put(ResourceResolverFactory.USER_IMPERSONATION, userImpersonationId);
        }
        return resourceResolverFactory.getServiceResourceResolver(params);
    }

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
