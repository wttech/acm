package com.wttech.aem.acm.core.osgi;

import com.wttech.aem.acm.core.instance.InstanceSettings;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = InstanceInfo.class)
public class InstanceInfo {

    private BundleContext bundleContext;

    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Reference
    private SlingSettingsService slingSettings;

    public boolean isAuthor() {
        return isRunMode(InstanceRole.AUTHOR.name());
    }

    public boolean isPublish() {
        return isRunMode(InstanceRole.PUBLISH.name());
    }

    public boolean isRunMode(String runMode) {
        return getRunModes().stream().anyMatch(rm -> StringUtils.equalsIgnoreCase(rm, runMode));
    }

    public Set<String> getRunModes() {
        return slingSettings.getRunModes();
    }

    public InstanceSettings getInstanceSettings() {
        boolean publish = isPublish();
        String propUrl = bundleContext.getProperty("PRODUCTINFO_VERSION");
        return new InstanceSettings(TimeZone.getDefault().getID(), publish, isCloudVersion(propUrl));
    }

    private boolean isCloudVersion(String version) {
        // For local instance treat it same as on-prem
        if (this.isRunMode("local")) {
            return false;
        }
        // Looks for pattern such as "YYYY."
        String regex = "^\\d{4}\\..*";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(version);
        return matcher.matches();
    }
}
