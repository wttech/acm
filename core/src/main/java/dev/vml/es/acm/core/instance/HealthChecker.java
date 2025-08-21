package dev.vml.es.acm.core.instance;

import dev.vml.es.acm.core.code.*;
import dev.vml.es.acm.core.osgi.*;
import dev.vml.es.acm.core.repo.Repo;
import dev.vml.es.acm.core.util.ResourceUtils;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyView;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.*;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = HealthChecker.class)
@Designate(ocd = HealthChecker.Config.class)
public class HealthChecker implements EventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HealthChecker.class);

    @Reference
    private OsgiScanner osgiScanner;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Executor executor;

    @Reference
    private InstanceInfo instanceInfo;

    @Reference
    private SlingInstaller slingInstaller;

    @Reference
    private DiscoveryService discoveryService;

    private Config config;

    private ServiceRegistration<EventHandler> eventHandlerRegistration;

    private OsgiEventCollector eventCollector;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        this.eventCollector = new OsgiEventCollector(config.eventQueueSize());

        unregisterEventHandler();
        registerEventHandler(config);
    }

    @Deactivate
    protected void deactivate() {
        unregisterEventHandler();
    }

    public HealthStatus checkStatus() {
        try (ResourceResolver resourceResolver = ResourceUtils.contentResolver(resourceResolverFactory, null)) {
            return checkStatus(resourceResolver);
        } catch (Exception e) {
            LOG.error("Health checker failed", e);
            return HealthStatus.exception(e);
        }
    }

    private HealthStatus checkStatus(ResourceResolver resourceResolver) {
        HealthStatus result = new HealthStatus();
        checkCluster(result);
        checkRepository(result, resourceResolver);
        checkInstaller(result, resourceResolver);
        checkBundles(result);
        checkEvents(result);
        checkComponents(result);
        checkCodeExecutor(result, resourceResolver);
        result.healthy = CollectionUtils.isEmpty(result.issues);
        return result;
    }

    private void checkCluster(HealthStatus result) {
        if (!instanceInfo.isCluster()) {
            return;
        }
        if (!isClusterLeader()) {
            result.issues.add(new HealthIssue(HealthIssueSeverity.CRITICAL, "Instance is not a cluster leader"));
        }
    }

    private boolean isClusterLeader() {
        return Optional.ofNullable(discoveryService)
                .map(DiscoveryService::getTopology)
                .map(TopologyView::getLocalInstance)
                .map(InstanceDescription::isLeader)
                .orElse(false);
    }

    // TODO seems to not work on AEMaaCS as there is no Sling Installer JMX MBean
    private void checkInstaller(HealthStatus result, ResourceResolver resourceResolver) {
        if (!config.installerChecking()) {
            return;
        }
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
        if (!config.repositoryChecking()) {
            return;
        }
        Repo repo = new Repo(resourceResolver);
        if ((instanceInfo.getType() == InstanceType.CLOUD_CONTAINER) && !repo.isCompositeNodeStore()) {
            result.issues.add(
                    new HealthIssue(HealthIssueSeverity.CRITICAL, "Repository is not yet using composite node store"));
        }
        if (ArrayUtils.isNotEmpty(config.repositoryPathsExisted())) {
            Arrays.stream(config.repositoryPathsExisted()).forEach(path -> {
                if (!repo.get(path).exists()) {
                    result.issues.add(new HealthIssue(
                            HealthIssueSeverity.CRITICAL, String.format("Repository path '%s' does not exist", path)));
                }
            });
        }
    }

    private void checkBundles(HealthStatus result) {
        if (!config.bundleChecking()) {
            return;
        }
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
        if (!config.eventChecking()) {
            return;
        }
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

    private void registerEventHandler(Config config) {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(EventConstants.EVENT_TOPIC, config.eventTopicsUnstable());
        eventHandlerRegistration = FrameworkUtil.getBundle(getClass())
                .getBundleContext()
                .registerService(EventHandler.class, this, properties);
    }

    private void unregisterEventHandler() {
        if (eventHandlerRegistration != null) {
            eventHandlerRegistration.unregister();
        }
    }

    private void checkComponents(HealthStatus result) {
        // TODO ...
    }

    private void checkCodeExecutor(HealthStatus result, ResourceResolver resourceResolver) {
        try (ExecutionContext context = executor.createContext(
                ExecutionId.generate(), ExecutionMode.RUN, Code.consoleMinimal(), resourceResolver)) {
            context.setHistory(false);
            Execution execution = executor.execute(context);
            if (execution.getStatus() != ExecutionStatus.SUCCEEDED) {
                result.issues.add(new HealthIssue(
                        HealthIssueSeverity.CRITICAL,
                        String.format("Code executor does not work: %s", execution.getError())));
            }
        } catch (Exception e) {
            result.issues.add(new HealthIssue(
                    HealthIssueSeverity.CRITICAL, String.format("Code executor does not work: %s", e.getMessage())));
        }
    }

    @ObjectClassDefinition(name = "AEM Content Manager - Health Checker")
    public @interface Config {

        @AttributeDefinition(name = "Bundle Checking")
        boolean bundleChecking() default true;

        @AttributeDefinition(
                name = "Bundle Symbolic Names Ignored",
                description = "Allows to exclude certain OSGi bundles from health check (to address known issues)")
        String[] bundleSymbolicNamesIgnored();

        @AttributeDefinition(name = "Event Checking")
        boolean eventChecking() default true;

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

        @AttributeDefinition(name = "Repository Checking")
        boolean repositoryChecking() default true;

        @AttributeDefinition(
                name = "Repository Paths Existed",
                description = "Paths to check for the existence in the repository")
        String[] repositoryPathsExisted();

        @AttributeDefinition(
                name = "Installer Checking",
                description = "Check if any CRX package is currently installed. Supported only on AEM On-Premise")
        boolean installerChecking() default false;
    }
}
