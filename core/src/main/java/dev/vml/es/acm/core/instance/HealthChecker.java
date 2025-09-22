package dev.vml.es.acm.core.instance;

import dev.vml.es.acm.core.code.*;
import dev.vml.es.acm.core.code.script.ExtensionScriptSyntax;
import dev.vml.es.acm.core.osgi.InstanceInfo;
import dev.vml.es.acm.core.osgi.InstanceType;
import dev.vml.es.acm.core.osgi.OsgiEvent;
import dev.vml.es.acm.core.osgi.OsgiEventCollector;
import dev.vml.es.acm.core.osgi.OsgiScanner;
import dev.vml.es.acm.core.osgi.OsgiUtils;
import dev.vml.es.acm.core.repo.Repo;
import dev.vml.es.acm.core.util.ExceptionUtils;
import dev.vml.es.acm.core.util.ResolverUtils;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.discovery.DiscoveryService;
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
        try (ResourceResolver resourceResolver = ResolverUtils.contentResolver(resourceResolverFactory, null)) {
            return checkStatus(resourceResolver);
        } catch (Exception e) {
            LOG.error("Health checker failed", e);
            return HealthStatus.exception(e);
        }
    }

    private HealthStatus checkStatus(ResourceResolver resourceResolver) {
        List<HealthIssue> issues = new LinkedList<>();
        checkRepository(issues, resourceResolver);
        checkInstaller(issues, resourceResolver);
        checkBundles(issues);
        checkEvents(issues);
        checkComponents(issues);
        checkCodeExecutor(issues, resourceResolver);
        return new HealthStatus(issues, CollectionUtils.isEmpty(issues));
    }

    // TODO seems to not work on AEMaaCS as there is no Sling Installer JMX MBean
    private void checkInstaller(List<HealthIssue> issues, ResourceResolver resourceResolver) {
        if (!config.installerChecking()) {
            return;
        }
        SlingInstallerState state = slingInstaller.checkState(resourceResolver);
        if (state.isActive()) {
            issues.add(new HealthIssue(
                    HealthIssueSeverity.CRITICAL,
                    HealthIssueCategory.INSTALLER,
                    String.format("Active resource count: %d", state.getActiveResourceCount()),
                    null));
        }
        if (state.isPaused()) {
            issues.add(new HealthIssue(
                    HealthIssueSeverity.CRITICAL,
                    HealthIssueCategory.INSTALLER,
                    String.format("Pause count: %d", state.getPauseCount()),
                    null));
        }
    }

    private void checkRepository(List<HealthIssue> issues, ResourceResolver resourceResolver) {
        if (!config.repositoryChecking()) {
            return;
        }
        Repo repo = new Repo(resourceResolver);
        if ((instanceInfo.getType() == InstanceType.CLOUD_CONTAINER) && !repo.isCompositeNodeStore()) {
            issues.add(new HealthIssue(
                    HealthIssueSeverity.CRITICAL,
                    HealthIssueCategory.REPOSITORY,
                    "Composite node store not available",
                    null));
        }
        if (ArrayUtils.isNotEmpty(config.repositoryPathsExisted())) {
            Arrays.stream(config.repositoryPathsExisted()).forEach(path -> {
                if (!repo.get(path).exists()) {
                    issues.add(new HealthIssue(
                            HealthIssueSeverity.CRITICAL,
                            HealthIssueCategory.REPOSITORY,
                            String.format("Path does not exist: '%s'", path),
                            null));
                }
            });
        }
    }

    /**
     * @see <https://github.com/apache/felix-dev/blob/master/framework/src/main/java/org/apache/felix/framework/BundleContextImpl.java> 'checkValidity()' method
     */
    private void checkBundles(List<HealthIssue> issues) {
        if (!config.bundleChecking()) {
            return;
        }

        try {
            osgiScanner.getBundleContext().getBundle();
        } catch (Exception e) {
            issues.add(new HealthIssue(
                    HealthIssueSeverity.CRITICAL,
                    HealthIssueCategory.OSGI,
                    "Bundle context not valid",
                    ExceptionUtils.toString(e)));
            return;
        }

        osgiScanner.scanBundles().filter(b -> !isBundleIgnored(b)).forEach(bundle -> {
            if (osgiScanner.isFragment(bundle)) {
                if (!osgiScanner.isBundleResolved(bundle)) {
                    issues.add(new HealthIssue(
                            HealthIssueSeverity.CRITICAL,
                            HealthIssueCategory.OSGI,
                            String.format(
                                    "Bundle fragment not resolved (%s): '%s'",
                                    OsgiUtils.bundleStateName(bundle.getState()), bundle.getSymbolicName()),
                            null));
                }
            } else {
                if (!osgiScanner.isBundleActive(bundle)) {
                    issues.add(new HealthIssue(
                            HealthIssueSeverity.CRITICAL,
                            HealthIssueCategory.OSGI,
                            String.format(
                                    "Bundle not active (%s): '%s'",
                                    OsgiUtils.bundleStateName(bundle.getState()), bundle.getSymbolicName()),
                            null));
                }
            }
        });
    }

    private boolean isBundleIgnored(Bundle bundle) {
        return ArrayUtils.isNotEmpty(config.bundleSymbolicNamesIgnored())
                && Arrays.stream(config.bundleSymbolicNamesIgnored())
                        .map(String::trim)
                        .anyMatch(sn -> FilenameUtils.wildcardMatch(bundle.getSymbolicName(), sn));
    }

    @Override
    public void handleEvent(Event event) {
        if (isEventUnstable(event)) {
            eventCollector.addEvent(event);
        }
    }

    private void checkEvents(List<HealthIssue> issues) {
        if (!config.eventChecking()) {
            return;
        }
        List<OsgiEvent> recentEvents = eventCollector.getRecentEvents(config.eventTimeWindow());
        if (!recentEvents.isEmpty()) {
            Map<String, Long> eventCounts =
                    recentEvents.stream().collect(Collectors.groupingBy(OsgiEvent::getTopic, Collectors.counting()));
            eventCounts.forEach((topic, count) -> issues.add(new HealthIssue(
                    HealthIssueSeverity.CRITICAL,
                    HealthIssueCategory.OSGI,
                    String.format("Event occurred (%d): %s", count, topic),
                    null)));
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

    private void checkComponents(List<HealthIssue> issues) {
        // TODO ...
    }

    private void checkCodeExecutor(List<HealthIssue> issues, ResourceResolver resourceResolver) {
        try (ExecutionContext context = executor.createContext(
                ExecutionId.generate(), ExecutionMode.RUN, Code.consoleMinimal(), resourceResolver)) {
            context.setHistory(false);
            context.setLocking(false);

            Execution execution = executor.execute(context);
            if (execution.getStatus() != ExecutionStatus.SUCCEEDED) {
                issues.add(new HealthIssue(
                        HealthIssueSeverity.CRITICAL,
                        HealthIssueCategory.CODE_EXECUTOR,
                        String.format(
                                "Execution not succeeded: %s",
                                execution.getStatus().name().toLowerCase()),
                        execution.getError()));
            }
        } catch (Exception e) {
            String error = ExceptionUtils.toString(e);
            String issue = StringUtils.contains(error, ExtensionScriptSyntax.MAIN_CLASS + ":")
                    ? "Extension script error"
                    : "Execution context error";
            issues.add(new HealthIssue(HealthIssueSeverity.CRITICAL, HealthIssueCategory.CODE_EXECUTOR, issue, error));
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
