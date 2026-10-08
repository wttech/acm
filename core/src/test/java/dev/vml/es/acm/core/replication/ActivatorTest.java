package dev.vml.es.acm.core.replication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import dev.vml.es.acm.core.AcmException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.jcr.Session;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ActivatorTest {

    private Session session;

    private Replicator replicator;

    private Activator activator;

    @BeforeEach
    void setUp() {
        session = mock(Session.class);
        replicator = mock(Replicator.class);
        ResourceResolver resolver = mock(ResourceResolver.class);
        when(resolver.adaptTo(Session.class)).thenReturn(session);
        activator = new Activator(resolver, replicator);
    }

    @Test
    void shouldReplicateNothingWithoutPaths() throws ReplicationException {
        activator.replicate(ReplicationActionType.ACTIVATE, false, 2);

        verifyNoInteractions(replicator);
    }

    @Test
    void shouldReplicateInFullChunks() throws ReplicationException {
        activator.replicate(ReplicationActionType.ACTIVATE, false, 2, "/a", "/b", "/c", "/d");

        assertEquals(
                Arrays.asList(Arrays.asList("/a", "/b"), Arrays.asList("/c", "/d")),
                replicatedChunks(ReplicationActionType.ACTIVATE, 2));
    }

    @Test
    void shouldReplicateRemainderInLastChunk() throws ReplicationException {
        activator.replicate(ReplicationActionType.DEACTIVATE, false, 2, "/a", "/b", "/c");

        assertEquals(
                Arrays.asList(Arrays.asList("/a", "/b"), Arrays.asList("/c")), replicatedChunks(ReplicationActionType.DEACTIVATE, 2));
    }

    @Test
    void shouldReplicateInOneChunkWhenChunkSizeExceedsPaths() throws ReplicationException {
        activator.replicate(ReplicationActionType.ACTIVATE, false, 50, "/a", "/b");

        assertEquals(Arrays.asList(Arrays.asList("/a", "/b")), replicatedChunks(ReplicationActionType.ACTIVATE, 1));
    }

    @Test
    void shouldPassSynchronousOption() throws ReplicationException {
        activator.replicate(ReplicationActionType.ACTIVATE, true, 1, "/a", "/b");

        ArgumentCaptor<ReplicationOptions> options = ArgumentCaptor.forClass(ReplicationOptions.class);
        verify(replicator, times(2))
                .replicate(eq(session), eq(ReplicationActionType.ACTIVATE), any(String[].class), options.capture());
        assertTrue(options.getAllValues().stream().allMatch(ReplicationOptions::isSynchronous));
    }

    @Test
    void shouldRejectChunkSizeLessThanOne() {
        assertThrows(AcmException.class, () -> activator.replicate(ReplicationActionType.ACTIVATE, false, 0, "/a"));

        verifyNoInteractions(replicator);
    }

    @Test
    void shouldStopAndReportFailedChunk() throws ReplicationException {
        ReplicationException cause = new ReplicationException("Agent unavailable");
        doNothing()
                .doThrow(cause)
                .when(replicator)
                .replicate(any(Session.class), any(ReplicationActionType.class), any(String[].class), any());

        AcmException e = assertThrows(
                AcmException.class,
                () -> activator.replicate(ReplicationActionType.ACTIVATE, false, 2, "/a", "/b", "/c", "/d", "/e"));

        assertSame(cause, e.getCause());
        assertTrue(e.getMessage().contains("chunk 2 of 3"), e.getMessage());
        assertTrue(e.getMessage().contains("[/c, /d]"), e.getMessage());
        assertEquals(
                Arrays.asList(Arrays.asList("/a", "/b"), Arrays.asList("/c", "/d")),
                replicatedChunks(ReplicationActionType.ACTIVATE, 2));
    }

    private List<List<String>> replicatedChunks(ReplicationActionType type, int count) throws ReplicationException {
        ArgumentCaptor<String[]> paths = ArgumentCaptor.forClass(String[].class);
        verify(replicator, times(count)).replicate(eq(session), eq(type), paths.capture(), any());
        return paths.getAllValues().stream().map(Arrays::asList).collect(Collectors.toList());
    }
}
