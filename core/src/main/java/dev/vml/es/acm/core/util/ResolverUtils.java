package dev.vml.es.acm.core.util;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.repo.RepoException;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.resource.LoginException;

public final class ResolverUtils {

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

    private ResolverUtils() {
        // intentionally empty
    }

    public static ResourceResolver contentResolver(
            ResourceResolverFactory resourceResolverFactory, String userImpersonationId) throws LoginException {
        return serviceResolver(resourceResolverFactory, Subservice.CONTENT, userImpersonationId);
    }

    public static ResourceResolver mockResolver(ResourceResolverFactory resourceResolverFactory) throws LoginException {
        return serviceResolver(resourceResolverFactory, Subservice.MOCK, null);
    }

    @SuppressWarnings("AEM Rules:AEM-11")
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
                throw new RepoException(String.format(
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
     * When a user is impersonated, resolver's user ID remains the same as the one of the service user.
     * To get the effective user ID, leverage the session user who effectively performs the operations on the repository.
     */
    public static String serviceOrImpersonatedUserId(ResourceResolver resourceResolver) {
        Session session = resourceResolver.adaptTo(Session.class);
        if (session == null) {
            throw new RepoException("Cannot determine service/impersonated user ID from resource resolver!");
        }
        return session.getUserID();
    }
}
