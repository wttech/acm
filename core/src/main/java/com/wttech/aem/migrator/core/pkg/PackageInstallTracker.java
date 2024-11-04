package com.wttech.aem.migrator.core.pkg;

import com.wttech.aem.migrator.core.MigratorException;
import com.wttech.aem.migrator.core.instance.HealthChecker;
import com.wttech.aem.migrator.core.script.ExecutionQueue;
import com.wttech.aem.migrator.core.script.Script;
import com.wttech.aem.migrator.core.script.ScriptRepository;
import java.util.*;
import java.util.stream.Collectors;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        immediate = true,
        service = {PackageInstallTracker.class, JobConsumer.class},
        property = {JobConsumer.PROPERTY_TOPICS + "=" + PackageInstallTracker.TOPIC})
public class PackageInstallTracker implements JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(PackageInstallTracker.class);

    public static final String TOPIC = "com/wttech/aem/migrator/PackageInstallTracker";

    @Reference
    private JobManager jobManager;

    @Reference
    private HealthChecker healthChecker;

    @Reference
    private Packaging packaging;

    @Reference
    private ExecutionQueue executionQueue;

    public void add(PackageInstallEvent event) {
        jobManager.addJob(TOPIC, event.toJobProps());
    }

    @Override
    public JobResult process(Job job) {
        var packageInstall = PackageInstallEvent.fromJob(job);

        ResourceResolver resourceResolver = null; // TODO get resource resolver
        Session session = resourceResolver.adaptTo(Session.class);

        Calendar installTimeBefore = readLastUnpacked(packageInstall.getPackageId(), session);
        // TODO wait for the package to be fully installed
        Calendar installTimeAfter = readLastUnpacked(packageInstall.getPackageId(), session);

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
        return null;
    }

    private Calendar readLastUnpacked(PackageId pid, Session session) {
        try (var pkg = packaging.getPackageManager(session).open(pid)) {
            return pkg.getDefinition().getLastUnpacked();
        } catch (RepositoryException e) {
            LOG.error("Error while opening package '{}'", pid, e);
            return null;
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
            executionQueue.add(script);
        } catch (MigratorException e) {
            LOG.error("Error while adding script to the queue", e);
        }
    }
}
