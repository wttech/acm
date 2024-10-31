package com.wttech.aem.migrator.core.pkg;

import com.wttech.aem.migrator.core.MigratorException;
import com.wttech.aem.migrator.core.instance.HealthChecker;
import com.wttech.aem.migrator.core.script.Queue;
import com.wttech.aem.migrator.core.script.Script;
import com.wttech.aem.migrator.core.script.ScriptRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.jackrabbit.vault.packaging.events.PackageEvent;
import org.apache.jackrabbit.vault.packaging.events.PackageEventListener;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO watch for AEM package installations containing migrator scripts TODO add to the queue all scripts from the
 * package TODO delay adding as as long as the package is being installed (check instance health)
 */
@Component(immediate = true, service = PackageEventListener.class)
public class PackageListener implements PackageEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(PackageListener.class);

    @Reference
    private Queue queue;

    @Reference
    private HealthChecker healthChecker;

    @Override
    public void onPackageEvent(PackageEvent packageEvent) {
        LOG.info("Detected package installation event: {}", packageEvent);

        if (PackageEvent.Type.INSTALL.equals(packageEvent.getType())) {
            return;
        }

        /*
        var pkgPid = packageEvent.getId().getName();
        LOG.info("Detected package PID '{}'", pkgPid);

        var scripts = lookupScriptsFromPackage(pkgPid);
        if (scripts.isEmpty()) {
            LOG.debug("No migration scripts found in package '{}'", pkgPid);
            return;
        }

        LOG.info("Found {} scripts in package '{}'", scripts.size(), pkgPid);
        */

        /* TODO ...
        while (!healthChecker.isHealthy()) {
            try {
                for (var script : lookupScriptsFromPackage(pkgPid)) {
                    queueScript(script);
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                LOG.error("Error while waiting for instance to become healthy", e);
            }
        }
        */
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
