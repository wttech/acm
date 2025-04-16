package com.vml.es.aem.acm.core.osgi;

import com.day.cq.replication.Replicator;
import com.vml.es.aem.acm.core.code.ExecutionQueue;
import java.util.Optional;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = OsgiContext.class)
public class OsgiContext {

    @Reference
    private InstanceInfo instanceInfo;

    private BundleContext bundleContext;

    @Activate
    @Modified
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public InstanceInfo getInstanceInfo() {
        return instanceInfo;
    }

    @SuppressWarnings("unchecked")
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

    public ExecutionQueue getExecutionQueue() {
        return getService(ExecutionQueue.class);
    }
}
