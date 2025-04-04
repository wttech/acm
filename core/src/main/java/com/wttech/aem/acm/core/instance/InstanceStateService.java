package com.wttech.aem.acm.core.instance;

import com.wttech.aem.acm.core.code.ExecutionQueue;
import com.wttech.aem.acm.core.osgi.OsgiContext;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = InstanceStateService.class)
public class InstanceStateService {
    @Reference
    private HealthChecker healthChecker;

    @Reference
    private ExecutionQueue executionQueue;

    @Reference
    private OsgiContext osgiContext;

    private static InstanceSettings instanceSettings;

    public HealthStatus getHealthStatus() {
        return healthChecker.checkStatus();
    }

    public InstanceSettings getInstanceSettings() {
        if (instanceSettings == null) {
            boolean publish = osgiContext.getInstanceInfo().isPublish();
            String propUrl = osgiContext.getBundleContext().getProperty("PRODUCTINFO_VERSION");
            instanceSettings = new InstanceSettings(TimeZone.getDefault().getID(), publish, isCloud(propUrl));
        }
        return instanceSettings;
    }

    private boolean isCloud(String version) {
        String regex = "^\\d{4}\\..*";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(version);
        return matcher.matches();
    }
}
