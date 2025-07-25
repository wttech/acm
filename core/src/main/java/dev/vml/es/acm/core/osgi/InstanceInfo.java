package dev.vml.es.acm.core.osgi;

import dev.vml.es.acm.core.instance.InstanceSettings;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@Component(immediate = true, service = InstanceInfo.class)
@Designate(ocd = InstanceInfo.Config.class)
public class InstanceInfo {

    private Config config;

    private BundleContext bundleContext;

    @Activate
    @Modified
    protected void activate(Config config, BundleContext bundleContext) {
        this.config = config;
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

    public InstanceRole getRole() {
        return isAuthor() ? InstanceRole.AUTHOR : InstanceRole.PUBLISH;
    }

    public InstanceType getType() {
        return InstanceType.of(config.type()).orElseGet(this::determineType);
    }

    private InstanceType determineType() {
        if (isCloudContainer()) {
            return InstanceType.CLOUD_CONTAINER;
        } else if (isCloudSdk()) {
            return InstanceType.CLOUD_SDK;
        } else {
            return InstanceType.ON_PREM;
        }
    }

    private boolean isCloudSdk() {
        String regex = config.versionCloudSdkRegex();
        Pattern pattern = Pattern.compile(regex);
        for (String prop : config.versionBundleContextProps()) {
            String value = bundleContext.getProperty(prop);
            if (StringUtils.isNotBlank(value)) {
                Matcher matcher = pattern.matcher(value);
                if (matcher.matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isCloudContainer() {
        for (String envProp : config.versionCloudContainerEnvProps()) {
            if (StringUtils.isNotBlank(System.getenv(envProp))) {
                return true;
            }
        }
        return false;
    }

    public boolean isCluster() {
        return isAuthor() && InstanceType.CLOUD_CONTAINER.equals(getType());
    }

    public boolean isRunMode(String runMode) {
        return getRunModes().stream().anyMatch(rm -> StringUtils.equalsIgnoreCase(rm, runMode));
    }

    public Set<String> getRunModes() {
        return slingSettings.getRunModes();
    }

    public InstanceSettings getInstanceSettings() {
        return new InstanceSettings(this);
    }

    @ObjectClassDefinition(name = "AEM Content Manager - Instance Info")
    public @interface Config {

        @AttributeDefinition(
                name = "Type",
                description =
                        "Determines the type of hardware the AEM instance is running on. Only when 'auto' is set, the other configuration values are in use.",
                options = {
                    @Option(label = "Determine automatically", value = "AUTO"),
                    @Option(label = "On Prem", value = "ON_PREM"),
                    @Option(label = "Cloud SDK", value = "CLOUD_SDK"),
                    @Option(label = "Cloud Container", value = "CLOUD_CONTAINER")
                })
        String type() default "AUTO";

        @AttributeDefinition(
                name = "Version Bundle Context Properties",
                description = "Used to determine the version of the AEM instance")
        String[] versionBundleContextProps() default {
            "PRODUCTINFO_VERSION", "PRODUCTINFO_SHORTVERSION", "granite.product.version"
        };

        @AttributeDefinition(
                name = "Version Cloud Regex",
                description = "Used to determine if the AEM instance is a cloud version (SDK or container)")
        String versionCloudSdkRegex() default "^\\d{4}\\..*";

        @AttributeDefinition(
                name = "Version Cloud Container Environment Properties",
                description = "Existence of these properties indicates that the instance is running on container")
        String[] versionCloudContainerEnvProps() default {"KUBERNETES_SERVICE_HOST", "KUBERNETES_SERVICE_PORT"};
    }
}
