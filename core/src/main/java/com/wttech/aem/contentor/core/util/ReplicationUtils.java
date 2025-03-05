package com.wttech.aem.contentor.core.util;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.day.jcr.vault.util.JcrConstants;
import com.wttech.aem.contentor.core.ContentorException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.jcr.Session;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public final class ReplicationUtils {

    private static final Set<String> ALLOWED_TYPES = new HashSet<>();

    static {
        ALLOWED_TYPES.add(JcrConstants.NT_FILE);
        ALLOWED_TYPES.add(JcrConstants.NT_FOLDER);
        ALLOWED_TYPES.add("sling:OrderedFolder");
        ALLOWED_TYPES.add("sling:Folder");
        ALLOWED_TYPES.add("dam:Asset");
        ALLOWED_TYPES.add("cq:Page");
    }

    private ReplicationUtils() {
        // intentionally empty
    }

    public static Session getSession(ResourceResolver resolver) {
        Session session =
                Optional.ofNullable(resolver).map(r -> r.adaptTo(Session.class)).orElse(null);
        if (session == null) {
            throw new ContentorException("Cannot access session!");
        }
        return session;
    }

    public static void publish(ResourceResolver resolver, Replicator replicator, String path) {
        try {
            Session session = getSession(resolver);
            replicator.replicate(session, ReplicationActionType.ACTIVATE, path);
        } catch (ReplicationException e) {
            throw new ContentorException(String.format("Cannot publish path '%s'", path), e);
        }
    }

    public static void publishTree(ResourceResolver resolver, Replicator replicator, String path) {
        Session session = getSession(resolver);
        Resource root = resolver.getResource(path);
        Optional.ofNullable(root)
                .map(ResourceSpliterator::stream)
                .orElse(Stream.empty())
                .filter(ReplicationUtils::isAllowed)
                .forEach(resource -> {
                    try {
                        replicator.replicate(session, ReplicationActionType.ACTIVATE, resource.getPath());
                    } catch (ReplicationException e) {
                        throw new ContentorException(String.format("Cannot publish path '%s'", resource.getPath()), e);
                    }
                });
    }

    public static void unpublish(ResourceResolver resolver, Replicator replicator, String path) {
        try {
            Session session = getSession(resolver);
            replicator.replicate(session, ReplicationActionType.DEACTIVATE, path);
        } catch (ReplicationException e) {
            throw new ContentorException(String.format("Cannot unpublish path '%s'", path), e);
        }
    }

    public static void delete(ResourceResolver resolver, Replicator replicator, String path) {
        try {
            Session session = getSession(resolver);
            replicator.replicate(session, ReplicationActionType.DELETE, path);
        } catch (ReplicationException e) {
            throw new ContentorException(String.format("Cannot delete path '%s'", path), e);
        }
    }

    private static boolean isAllowed(Resource resource) {
        return ALLOWED_TYPES.stream().anyMatch(resource::isResourceType);
    }
}
