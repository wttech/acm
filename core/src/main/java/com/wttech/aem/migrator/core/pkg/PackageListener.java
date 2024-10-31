package com.wttech.aem.migrator.core.pkg;

import com.wttech.aem.migrator.core.MigratorException;
import com.wttech.aem.migrator.core.instance.HealthChecker;
import com.wttech.aem.migrator.core.script.Queue;
import com.wttech.aem.migrator.core.script.Script;
import com.wttech.aem.migrator.core.script.ScriptRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO watch for AEM package installations containing migrator scripts TODO add to the queue all scripts from the
 * package TODO delay adding as as long as the package is being installed (check instance health)
 */
@Component(immediate = true, service = EventHandler.class)
public class PackageListener implements EventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PackageListener.class);

    @Reference
    private Queue queue;

    @Reference
    private HealthChecker healthChecker;

    @Override
    public void handleEvent(Event event) {
        while (!healthChecker.isHealthy()) {
            try {
                var pkgPid = event.getProperty("pid").toString();
                for (var script : lookupScriptsFromPackage(pkgPid)) {
                    queueScript(script);
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                LOG.error("Error while waiting for instance to become healthy", e);
            }
        }
    }

    private List<Script> lookupScriptsFromPackage(String pkgPid) {
        ResourceResolver resourceResolver = null; // TODO get resource resolver

        var pkgRepo = new PackageRepository(resourceResolver);
        var pkg = pkgRepo.read(pkgPid);
        if (pkg.isEmpty()) {
            return List.of();
        }

        var scriptRepo = new ScriptRepository(resourceResolver);
        var scriptPaths = pkg.get().findScriptPaths();

        return scriptPaths.stream()
                .map(scriptRepo::read)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private void queueScript(Script script) {
        try {
            queue.add(script);
        } catch (MigratorException e) {
            LOG.error("Error while adding script to the queue", e);
        }
    }
}
