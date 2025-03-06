package com.wttech.aem.contentor.core.instance;

import com.wttech.aem.contentor.core.osgi.OsgiScanner;
import org.apache.commons.collections.CollectionUtils;
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
        osgiScanner.scanBundles().forEach(bundle -> {
            if (osgiScanner.isFragment(bundle)) {
                if (!osgiScanner.isBundleResolved(bundle)) {
                    result.issues.add(new HealthIssue(
                            String.format("Bundle fragment '%s' is not resolved!", bundle.getSymbolicName())));
                }
            } else {
                if (!osgiScanner.isBundleActive(bundle)) {
                    result.issues.add(
                            new HealthIssue(String.format("Bundle '%s' is not active!", bundle.getSymbolicName())));
                }
            }
        });
        result.healthy = CollectionUtils.isEmpty(result.issues);
        return result;
    }

    @ObjectClassDefinition(name = "AEM Contentor - Health Checker")
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
