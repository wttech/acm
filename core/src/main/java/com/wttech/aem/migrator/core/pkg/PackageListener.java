package com.wttech.aem.migrator.core.pkg;

import java.time.LocalDateTime;
import org.apache.jackrabbit.vault.packaging.events.PackageEvent;
import org.apache.jackrabbit.vault.packaging.events.PackageEventListener;
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
    private PackageInstallTracker packageInstallWatcher;

    @Override
    public void onPackageEvent(PackageEvent packageEvent) {
        if (!PackageEvent.Type.INSTALL.equals(packageEvent.getType())) {
            return;
        }

        // TODO some filtering logic here? to track only own packages? or 'migrable' based on some metadata

        LOG.info("Detected package installation event: {}", packageEvent);
        var event = new PackageInstallEvent(LocalDateTime.now(), packageEvent.getId());
        packageInstallWatcher.add(event);
    }
}
