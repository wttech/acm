package com.wttech.aem.acm.core.instance;

import com.wttech.aem.acm.core.osgi.OsgiEvent;
import com.wttech.aem.acm.core.osgi.OsgiEventCollector;
import com.wttech.aem.acm.core.osgi.OsgiScanner;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
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

    private Config config;

    private ServiceRegistration<EventHandler> eventHandlerRegistration;

    private OsgiEventCollector eventCollector;

    @Activate
    @Modified
    protected void activate(BundleContext context, Config config) {
        this.config = config;
        this.eventCollector = new OsgiEventCollector(config.maxEventSize());

        unregisterEventHandler();
        registerEventHandler(context, config);
    }

    @Deactivate
    protected void deactivate() {
        unregisterEventHandler();
    }

    public HealthStatus checkStatus() {
        HealthStatus result = new HealthStatus();
        checkBundles(result);
        checkEvents(result);
        result.healthy = CollectionUtils.isEmpty(result.issues);
        return result;
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

    @ObjectClassDefinition(name = "AEM Content Manager - Health Checker")
    public @interface Config {

        @AttributeDefinition(name = "Bundle Symbolic Names Ignored")
        String[] bundleSymbolicNamesIgnored();

        @AttributeDefinition(name = "Event Unstable Topics")
        String[] eventTopicsUnstable() default {
            "org/osgi/framework/ServiceEvent/*",
            "org/osgi/framework/FrameworkEvent/*",
            "org/osgi/framework/BundleEvent/*"
        };

        @AttributeDefinition(name = "Event Unstable Time Window (ms)")
        long eventTimeWindow() default 1000 * 10;

        @AttributeDefinition(name = "Max Event Size")
        int maxEventSize() default 1000;
    }
}
