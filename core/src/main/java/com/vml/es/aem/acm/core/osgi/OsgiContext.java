package com.vml.es.aem.acm.core.osgi;

import com.day.cq.replication.Replicator;
import com.vml.es.aem.acm.core.code.ExecutionQueue;
import java.util.Optional;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

@Component(immediate = true, service = OsgiContext.class)
public class OsgiContext {

    private BundleContext bundleContext;

    @Activate
    @Modified
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public <T> Optional<T> findService(Class<T> clazz) {
        return Optional.ofNullable(clazz)
                .map(c -> bundleContext.getServiceReference(clazz))
                .map(sf -> bundleContext.getService(sf));
    }

    public <T> T getService(Class<T> clazz) {
        return findService(clazz).orElseThrow(() -> new IllegalStateException("Service not found: " + clazz.getName()));
    }

    public Replicator getReplicator() {
        return getService(Replicator.class);
    }

    public OsgiScanner getOsgiScanner() {
        return getService(OsgiScanner.class);
    }

    public InstanceInfo getInstanceInfo() {
        return getService(InstanceInfo.class);
    }

    public ExecutionQueue getExecutionQueue() {
        return getService(ExecutionQueue.class);
    }
}
