package dev.vml.es.acm.core.osgi;

import com.day.cq.replication.Replicator;
import dev.vml.es.acm.core.code.ExecutionQueue;
import dev.vml.es.acm.core.script.ScriptScheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

@Component(immediate = true, service = OsgiContext.class)
public class OsgiContext {

    private BundleContext bundleContext;

    private LocalDateTime modified;

    @Activate
    @Modified
    protected void activate(BundleContext bundleContext) {
        this.modified = LocalDateTime.now();
        this.bundleContext = bundleContext;
    }

    public LocalDateTime getModified() {
        return modified;
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

    public ScriptScheduler getScriptScheduler() {
        return getService(ScriptScheduler.class);
    }

    public ExecutionQueue getExecutionQueue() {
        return getService(ExecutionQueue.class);
    }

    public String readInstanceState() {
        return String.format("modified=%s;bundles=%s",
                getModified().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                getOsgiScanner().computeBundlesHashCode()
        );
    }
}
