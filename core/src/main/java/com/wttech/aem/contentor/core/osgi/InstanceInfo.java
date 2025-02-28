package com.wttech.aem.contentor.core.osgi;

import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = InstanceInfo.class)
public class InstanceInfo {

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
}
