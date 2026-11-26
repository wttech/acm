package dev.vml.es.acm.core.code;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyView;
import org.junit.jupiter.api.Test;

class LockOwnerPresenceTest {

    private static final String LOCAL_INSTANCE = "local-instance";

    private static final String OWNER_INSTANCE = "owner-instance";

    private final DiscoveryService discovery = mock(DiscoveryService.class);

    @Test
    void shouldTreatOwnerAsGoneWhenAbsentFromCurrentClusterView() {
        clusterView(true, LOCAL_INSTANCE);

        assertTrue(new LockOwnerPresence(discovery).hasLeftCluster(OWNER_INSTANCE));
    }

    @Test
    void shouldTreatOwnerAsPresentWhenInClusterView() {
        clusterView(true, LOCAL_INSTANCE, OWNER_INSTANCE);

        assertFalse(new LockOwnerPresence(discovery).hasLeftCluster(OWNER_INSTANCE));
    }

    @Test
    void shouldTreatOwnerAsPresentWhenViewIsNotCurrent() {
        clusterView(false, LOCAL_INSTANCE);

        assertFalse(new LockOwnerPresence(discovery).hasLeftCluster(OWNER_INSTANCE));
    }

    @Test
    void shouldTreatOwnerAsPresentWhenDiscoveryIsMissing() {
        assertFalse(new LockOwnerPresence(null).hasLeftCluster(OWNER_INSTANCE));
    }

    @Test
    void shouldTreatOwnerAsPresentWhenDiscoveryFails() {
        when(discovery.getTopology()).thenThrow(new IllegalStateException("Discovery stopped"));

        assertFalse(new LockOwnerPresence(discovery).hasLeftCluster(OWNER_INSTANCE));
    }

    @Test
    void shouldTreatLocalOwnerAsPresent() {
        clusterView(true);
        when(discovery.getTopology().getLocalInstance().getSlingId()).thenReturn(OWNER_INSTANCE);

        assertFalse(new LockOwnerPresence(discovery).hasLeftCluster(OWNER_INSTANCE));
    }

    @Test
    void shouldTreatMissingOwnerAsPresent() {
        clusterView(true, LOCAL_INSTANCE);

        assertFalse(new LockOwnerPresence(discovery).hasLeftCluster(null));
    }

    @Test
    void shouldSeeOtherInstanceInCurrentClusterViewAsInCluster() {
        clusterView(true, LOCAL_INSTANCE, OWNER_INSTANCE);

        assertTrue(new LockOwnerPresence(discovery).isOtherInstanceInCluster(OWNER_INSTANCE));
    }

    @Test
    void shouldNotSeeLocalInstanceAsOtherInstanceInCluster() {
        clusterView(true, LOCAL_INSTANCE);

        assertFalse(new LockOwnerPresence(discovery).isOtherInstanceInCluster(LOCAL_INSTANCE));
    }

    @Test
    void shouldNotSeeInstanceAbsentFromClusterViewAsInCluster() {
        clusterView(true, LOCAL_INSTANCE);

        assertFalse(new LockOwnerPresence(discovery).isOtherInstanceInCluster(OWNER_INSTANCE));
    }

    @Test
    void shouldNotSeeOtherInstanceAsInClusterWhenViewIsNotCurrent() {
        clusterView(false, LOCAL_INSTANCE, OWNER_INSTANCE);

        assertFalse(new LockOwnerPresence(discovery).isOtherInstanceInCluster(OWNER_INSTANCE));
    }

    @Test
    void shouldNotSeeOtherInstanceAsInClusterWithoutDiscovery() {
        assertFalse(new LockOwnerPresence(null).isOtherInstanceInCluster(OWNER_INSTANCE));
    }

    @Test
    void shouldNotSeeMissingOwnerAsInCluster() {
        clusterView(true, LOCAL_INSTANCE, OWNER_INSTANCE);

        assertFalse(new LockOwnerPresence(discovery).isOtherInstanceInCluster(null));
    }

    @Test
    void shouldReadLocalSlingIdFromTopology() {
        clusterView(true, LOCAL_INSTANCE);

        assertEquals(LOCAL_INSTANCE, new LockOwnerPresence(discovery).localSlingId());
    }

    @Test
    void shouldHaveNoLocalSlingIdWhenViewIsNotCurrent() {
        clusterView(false, LOCAL_INSTANCE);

        assertNull(new LockOwnerPresence(discovery).localSlingId());
    }

    @Test
    void shouldHaveNoLocalSlingIdWhenDiscoveryIsMissing() {
        assertNull(new LockOwnerPresence(null).localSlingId());
    }

    private void clusterView(boolean current, String... slingIds) {
        Set<InstanceDescription> instances =
                Arrays.stream(slingIds).map(this::instance).collect(Collectors.toSet());
        InstanceDescription local = instance(LOCAL_INSTANCE);
        TopologyView view = mock(TopologyView.class);
        when(view.isCurrent()).thenReturn(current);
        when(view.getLocalInstance()).thenReturn(local);
        when(view.getInstances()).thenReturn(instances);
        when(discovery.getTopology()).thenReturn(view);
    }

    private InstanceDescription instance(String slingId) {
        InstanceDescription instance = mock(InstanceDescription.class);
        when(instance.getSlingId()).thenReturn(slingId);
        return instance;
    }
}
