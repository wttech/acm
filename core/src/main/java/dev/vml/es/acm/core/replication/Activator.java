package dev.vml.es.acm.core.replication;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.util.ResourceSpliterator;
import java.util.Optional;
import java.util.stream.Stream;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public class Activator {

    private final ResourceResolver resolver;

    private final Session session;

    private final com.day.cq.replication.Replicator replicator;

    public Activator(ResourceResolver resolver, Replicator replicator) {
        this.resolver = resolver;
        this.session = Optional.ofNullable(resolver)
                .map(r -> r.adaptTo(Session.class))
                .orElseThrow(() -> new AcmException("Cannot access session!"));
        this.replicator = replicator;
    }

    public void activate(String path) {
        try {
            replicator.replicate(session, ReplicationActionType.ACTIVATE, path);
        } catch (ReplicationException e) {
            throw new AcmException(String.format("Cannot publish path '%s'", path), e);
        }
    }

    public void deactivate(String path) {
        try {
            replicator.replicate(session, ReplicationActionType.DEACTIVATE, path);
        } catch (ReplicationException e) {
            throw new AcmException(String.format("Cannot publish path '%s'", path), e);
        }
    }

    public void activateTree(String path) {
        Optional.ofNullable(path)
                .map(resolver::getResource)
                .map(root -> ResourceSpliterator.stream(root, this::traversePredicate))
                .orElse(Stream.empty())
                .forEach(resource -> activate(resource.getPath()));
    }

    private boolean traversePredicate(Resource resource) {
        try {
            Node node = resource.adaptTo(Node.class);
            return node != null && node.isNodeType("nt:hierarchyNode");
        } catch (RepositoryException e) {
            throw new AcmException("Cannot get node type", e);
        }
    }

    public void reactivate(String path) {
        deactivate(path);
        activate(path);
    }

    public void reactivateTree(String path) {
        deactivate(path);
        activateTree(path);
    }
}
