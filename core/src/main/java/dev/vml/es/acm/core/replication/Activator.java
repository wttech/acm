package dev.vml.es.acm.core.replication;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.osgi.OsgiContext;
import dev.vml.es.acm.core.util.ResourceSpliterator;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator {

    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    private final ResourceResolver resolver;

    private final Session session;

    private final com.day.cq.replication.Replicator replicator;

    public Activator(ResourceResolver resolver, OsgiContext osgiContext) {
        this(resolver, osgiContext.getService(Replicator.class));
    }

    public Activator(ResourceResolver resolver, Replicator replicator) {
        this.resolver = resolver;
        this.session = Optional.ofNullable(resolver)
                .map(r -> r.adaptTo(Session.class))
                .orElseThrow(() -> new AcmException("Cannot access session!"));
        this.replicator = replicator;
    }

    public void activate(String path) {
        replicateSinglePath(ReplicationActionType.ACTIVATE, path);
    }

    public void deactivate(String path) {
        replicateSinglePath(ReplicationActionType.DEACTIVATE, path);
    }

    public void activateTree(String path) {
        Optional.ofNullable(path)
                .map(resolver::getResource)
                .map(root -> ResourceSpliterator.stream(root, this::traversePredicate))
                .orElse(Stream.empty())
                .forEach(resource -> activate(resource.getPath()));
    }

    public void replicate(
            ReplicationActionType replicationActionType, boolean synchronous, int chunkSize, String... paths) {
        List<List<String>> chunks = chunk(paths, chunkSize);
        ReplicationOptions options = new ReplicationOptions();
        options.setSynchronous(synchronous);
        int counter = 0;
        try {
            for (List<String> chunk : chunks) {
                replicator.replicate(session, replicationActionType, chunk.toArray(new String[] {}), options);
                counter++;
                LOG.info(
                        "Replicated chunk {} of {} ({} paths). Replication action: {}. Synchronous: {}.",
                        counter,
                        chunks.size(),
                        chunk.size(),
                        replicationActionType,
                        synchronous);
            }
        } catch (ReplicationException e) {
            throw new AcmException(
                    String.format(
                            "Cannot '%s' chunk %d of %d. Paths '%s'",
                            replicationActionType, counter + 1, chunks.size(), chunks.get(counter)),
                    e);
        }
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

    private void replicateSinglePath(ReplicationActionType replicationActionType, String path) {
        try {
            replicator.replicate(session, replicationActionType, path);
        } catch (ReplicationException e) {
            throw new AcmException(String.format("Cannot '%s' path '%s'", replicationActionType, path), e);
        }
    }

    private static List<List<String>> chunk(String[] array, int chunkSize) {
        if (chunkSize < 1) {
            throw new AcmException("Replication chunk size can't be less than 1!");
        }
        return IntStream.range(0, (array.length + chunkSize - 1) / chunkSize)
                .mapToObj(i -> Arrays.asList(
                        Arrays.copyOfRange(array, i * chunkSize, Math.min((i + 1) * chunkSize, array.length))))
                .collect(Collectors.toList());
    }
}
