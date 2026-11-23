package dev.vml.es.acm.core.code;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.TopologyView;

/** Tells whether the instance that holds an execution lock has left the topology, so it can no longer write. */
class LockOwnerPresence {

    private final DiscoveryService discovery;

    LockOwnerPresence(DiscoveryService discovery) {
        this.discovery = discovery;
    }

    /** Null while the instance is not in a current view yet: other instances may not see it as present. */
    String localSlingId() {
        TopologyView topology = readTopology();
        if (topology == null || !topology.isCurrent()) {
            return null;
        }
        return topology.getLocalInstance().getSlingId();
    }

    /** Only a current topology view without the owner proves it is gone. Anything unknown counts as present. */
    boolean hasLeftCluster(String ownerSlingId) {
        if (StringUtils.isBlank(ownerSlingId)) {
            return false;
        }
        TopologyView topology = readTopology();
        if (topology == null || !topology.isCurrent()) {
            return false;
        }
        if (ownerSlingId.equals(topology.getLocalInstance().getSlingId())) {
            return false;
        }
        return topology.getInstances().stream().noneMatch(instance -> ownerSlingId.equals(instance.getSlingId()));
    }

    /** Only a current topology view with the owner proves that its run may still write. */
    boolean isOtherInstanceInCluster(String ownerSlingId) {
        if (StringUtils.isBlank(ownerSlingId)) {
            return false;
        }
        TopologyView topology = readTopology();
        if (topology == null || !topology.isCurrent()) {
            return false;
        }
        if (ownerSlingId.equals(topology.getLocalInstance().getSlingId())) {
            return false;
        }
        return topology.getInstances().stream().anyMatch(instance -> ownerSlingId.equals(instance.getSlingId()));
    }

    private TopologyView readTopology() {
        if (discovery == null) {
            return null;
        }
        try {
            return discovery.getTopology();
        } catch (RuntimeException e) {
            return null; // discovery stops together with the instance
        }
    }
}
