package com.wttech.aem.acm.core.instance;

import com.wttech.aem.acm.core.osgi.OsgiScanner;
import java.util.Arrays;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(immediate = true, service = HealthChecker.class)
@Designate(ocd = HealthChecker.Config.class)
public class HealthChecker {

    private Config config;

    @Reference
    private OsgiScanner osgiScanner;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
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
                            String.format("Bundle fragment '%s' is not resolved!", bundle.getSymbolicName())));
                }
            } else {
                if (!osgiScanner.isBundleActive(bundle)) {
                    result.issues.add(new HealthIssue(
                            HealthIssueSeverity.CRITICAL,
                            String.format("Bundle '%s' is not active!", bundle.getSymbolicName())));
                }
            }
        });
    }

    private boolean isBundleIgnored(Bundle bundle) {
        return ArrayUtils.isNotEmpty(config.bundleSymbolicNamesIgnored())
                && Arrays.stream(config.bundleSymbolicNamesIgnored())
                        .anyMatch(sn -> FilenameUtils.wildcardMatch(bundle.getSymbolicName(), sn));
    }

    /**
     * https://github.com/apache/felix-dev/blob/master/webconsole-plugins/event/src/main/java/org/apache/felix/webconsole/plugins/event/internal/EventHandler.java
     * https://github.com/apache/felix-dev/blob/master/webconsole-plugins/event/src/main/java/org/apache/felix/webconsole/plugins/event/internal/EventCollector.java
     */
    private void checkEvents(HealthStatus result) {
        // TODO implement OSGi events checking; leverage 'config.eventTopicsUnstable'
    }

    @ObjectClassDefinition(name = "AEM Content Manager - Health Checker")
    public @interface Config {

        @AttributeDefinition(name = "Bundle Symbolic Names Ignored")
        String[] bundleSymbolicNamesIgnored();

        @AttributeDefinition(name = "Event Topics Unstable")
        String[] eventTopicsUnstable() default {
            "org/osgi/framework/ServiceEvent/*",
            "org/osgi/framework/FrameworkEvent/*",
            "org/osgi/framework/BundleEvent/*"
        };
    }
}
