package com.wttech.aem.contentor.core.util;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.wttech.aem.contentor.core.ContentorException;
import java.util.Optional;
import java.util.stream.Stream;
import javax.jcr.Session;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public final class ReplicationUtils {

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
        Session session = getSession(resolver);
        publish(session, replicator, path);
    }

    public static void publishTree(ResourceResolver resolver, Replicator replicator, String path) {
        Session session = getSession(resolver);
        Resource root = resolver.getResource(path);
        Optional.ofNullable(root)
                .map(ResourceSpliterator::stream)
                .orElse(Stream.empty())
                .forEach(resource -> publish(session, replicator, resource.getPath()));
    }

    private static void publish(Session session, Replicator replicator, String path) {
        try {
            replicator.replicate(session, ReplicationActionType.ACTIVATE, path);
        } catch (ReplicationException e) {
            throw new ContentorException(String.format("Cannot publish path '%s'", path), e);
        }
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
}
