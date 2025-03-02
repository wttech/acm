package com.wttech.aem.contentor.core.util;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

public final class ResourceUtils {

    private ResourceUtils() {
        // intentionally empty
    }

    public static ResourceResolver serviceResolver(ResourceResolverFactory resourceResolverFactory)
            throws LoginException {
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put(ResourceResolverFactory.SUBSERVICE, "contentor");
        return resourceResolverFactory.getServiceResourceResolver(serviceParams);
    }

    public static void move(ResourceResolver resolver, Replicator replicator, String sourcePath, String targetPath)
            throws PersistenceException {
        Session session =
                Optional.ofNullable(resolver).map(r -> r.adaptTo(Session.class)).orElse(null);
        Workspace workspace =
                Optional.ofNullable(session).map(Session::getWorkspace).orElse(null);
        if (workspace == null) {
            throw new PersistenceException(String.format(
                    "Cannot move resource from '%s' to '%s' as cannot access workspace!", sourcePath, targetPath));
        }
        try {
            workspace.move(sourcePath, targetPath);
            if (replicator != null) {
                replicator.replicate(session, ReplicationActionType.ACTIVATE, targetPath);
                replicator.replicate(session, ReplicationActionType.DELETE, sourcePath);
            }
        } catch (RepositoryException | ReplicationException e) {
            throw new PersistenceException(
                    String.format(
                            "Cannot move resource from '%s' to '%s' due to workspace move error!",
                            sourcePath, targetPath),
                    e);
        }
    }
}
