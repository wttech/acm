package com.wttech.aem.acm.core.instance;

import com.wttech.aem.acm.core.code.ExecutionQueue;
import com.wttech.aem.acm.core.code.ExecutionSummary;
import com.wttech.aem.acm.core.osgi.OsgiContext;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = StateService.class)
public class StateService {
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
        // For local instance treat it same as on-prem
        if (osgiContext.getInstanceInfo().isRunMode("local")) {
            return false;
        }
        String regex = "^\\d{4}\\..*";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(version);
        return matcher.matches();
    }

    public List<ExecutionSummary> getQueuedExecutions() {
        return executionQueue.findAllSummaries().collect(Collectors.toList());
    }
}
