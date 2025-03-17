package com.wttech.aem.acm.core.instance;

import com.wttech.aem.acm.core.osgi.OsgiEvent;
import com.wttech.aem.acm.core.osgi.OsgiEventCollector;
import com.wttech.aem.acm.core.osgi.OsgiScanner;
import com.wttech.aem.acm.core.repository.Repository;
import com.wttech.aem.acm.core.util.ResourceUtils;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.*;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(immediate = true, service = HealthChecker.class)
@Designate(ocd = HealthChecker.Config.class)
public class HealthChecker implements EventHandler {

    @Reference
    private OsgiScanner osgiScanner;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private SlingInstaller slingInstaller;

    private Config config;

    private ServiceRegistration<EventHandler> eventHandlerRegistration;

    private OsgiEventCollector eventCollector;

    @Activate
    @Modified
    protected void activate(BundleContext context, Config config) {
        this.config = config;
        this.eventCollector = new OsgiEventCollector(config.eventQueueSize());

        unregisterEventHandler();
        registerEventHandler(context, config);
    }

    @Deactivate
    protected void deactivate() {
        unregisterEventHandler();
    }

    public HealthStatus checkStatus() {
        try (ResourceResolver resourceResolver = ResourceUtils.serviceResolver(resourceResolverFactory)) {
            return checkStatus(resourceResolver);
        } catch (Exception e) {
            return HealthStatus.error(e);
        }
    }

    private HealthStatus checkStatus(ResourceResolver resourceResolver) {
        HealthStatus result = new HealthStatus();
        checkRepository(result, resourceResolver);
        checkInstaller(result, resourceResolver);
        checkBundles(result);
        checkEvents(result);
        checkComponents(result);
        result.healthy = CollectionUtils.isEmpty(result.issues);
        return result;
    }

    private void checkInstaller(HealthStatus result, ResourceResolver resourceResolver) {
        SlingInstallerState state = slingInstaller.checkState(resourceResolver);
        if (state.isActive()) {
            result.issues.add(new HealthIssue(
                    HealthIssueSeverity.CRITICAL,
                    String.format("Sling Installer is active (%d)", state.getActiveResourceCount())));
        }
        if (state.isPaused()) {
            result.issues.add(new HealthIssue(
                    HealthIssueSeverity.CRITICAL,
                    String.format("Sling Installer is paused (%d)", state.getPauseCount())));
        }
    }

    private void checkRepository(HealthStatus result, ResourceResolver resourceResolver) {
        Repository repository = new Repository(resourceResolver);
        if (repository.isCompositeNodeStore()) {
            result.issues.add(new HealthIssue(
                    HealthIssueSeverity.CRITICAL, "Repository with composite node store is not supported"));
        }
        if (ArrayUtils.isNotEmpty(config.repositoryPathsExisted())) {
            Arrays.stream(config.repositoryPathsExisted()).forEach(path -> {
                if (!repository.exists(path)) {
                    result.issues.add(new HealthIssue(
                            HealthIssueSeverity.CRITICAL, String.format("Repository path '%s' does not exist", path)));
                }
            });
        }
    }

    private void checkBundles(HealthStatus result) {
        osgiScanner.scanBundles().filter(b -> !isBundleIgnored(b)).forEach(bundle -> {
            if (osgiScanner.isFragment(bundle)) {
                if (!osgiScanner.isBundleResolved(bundle)) {
                    result.issues.add(new HealthIssue(
                            HealthIssueSeverity.CRITICAL,
                            String.format("Bundle fragment '%s' is not resolved", bundle.getSymbolicName())));
                }
            } else {
                if (!osgiScanner.isBundleActive(bundle)) {
                    result.issues.add(new HealthIssue(
                            HealthIssueSeverity.CRITICAL,
                            String.format("Bundle '%s' is not active", bundle.getSymbolicName())));
                }
            }
        });
    }

    private boolean isBundleIgnored(Bundle bundle) {
        return ArrayUtils.isNotEmpty(config.bundleSymbolicNamesIgnored())
                && Arrays.stream(config.bundleSymbolicNamesIgnored())
                        .anyMatch(sn -> FilenameUtils.wildcardMatch(bundle.getSymbolicName(), sn));
    }

    @Override
    public void handleEvent(Event event) {
        if (isEventUnstable(event)) {
            eventCollector.addEvent(event);
        }
    }

    private void checkEvents(HealthStatus result) {
        List<OsgiEvent> recentEvents = eventCollector.getRecentEvents(config.eventTimeWindow());
        if (!recentEvents.isEmpty()) {
            Map<String, Long> eventCounts =
                    recentEvents.stream().collect(Collectors.groupingBy(OsgiEvent::getTopic, Collectors.counting()));
            eventCounts.forEach((topic, count) -> result.issues.add(new HealthIssue(
                    HealthIssueSeverity.CRITICAL, String.format("Event '%s' occurred (%d)", topic, count))));
        }
    }

    private boolean isEventUnstable(Event event) {
        return Arrays.stream(config.eventTopicsUnstable())
                .anyMatch(etu -> FilenameUtils.wildcardMatch(event.getTopic(), etu));
    }

    private void registerEventHandler(BundleContext context, Config config) {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(EventConstants.EVENT_TOPIC, config.eventTopicsUnstable());
        eventHandlerRegistration = context.registerService(EventHandler.class, this, properties);
    }

    private void unregisterEventHandler() {
        if (eventHandlerRegistration != null) {
            eventHandlerRegistration.unregister();
        }
    }

    private void checkComponents(HealthStatus result) {
        // TODO ...
    }

    @ObjectClassDefinition(name = "AEM Content Manager - Health Checker")
    public @interface Config {

        @AttributeDefinition(
                name = "Bundle Symbolic Names Ignored",
                description = "Allows to exclude certain OSGi bundles from health check (to address known issues)")
        String[] bundleSymbolicNamesIgnored();

        @AttributeDefinition(
                name = "Event Unstable Topics",
                description = "Allows to specify OSGi event topics to be considered unstable")
        String[] eventTopicsUnstable() default {
            "org/osgi/framework/ServiceEvent/*",
            "org/osgi/framework/FrameworkEvent/*",
            "org/osgi/framework/BundleEvent/*"
        };

        @AttributeDefinition(
                name = "Event Unstable Time Window (ms)",
                description = "Max age of unstable events to be considered")
        long eventTimeWindow() default 1000 * 10;

        @AttributeDefinition(name = "Event Unstable Queue Size", description = "Max number of unstable events to store")
        int eventQueueSize() default 250;

        @AttributeDefinition(
                name = "Repository Paths Existed",
                description = "Paths to check for the existence in the repository")
        String[] repositoryPathsExisted();
    }
}
