package com.wttech.aem.contentor.core.osgi;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * @see "https://github.com/Adobe-Consulting-Services/acs-aem-commons/issues/2476#issuecomment-2315365114"
 */
@Component(immediate = true, service = InstanceManager.class)
@Designate(ocd = InstanceManager.Config.class)
public class InstanceManager {

    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
    }

    public boolean isAuthor() {
        return InstanceRole.AUTHOR.name().equalsIgnoreCase(config.role());
    }

    public boolean isPublish() {
        return InstanceRole.PUBLISH.name().equalsIgnoreCase(config.role());
    }

    @ObjectClassDefinition(name = "AEM Contentor - Instance Manager")
    public @interface Config {

        @AttributeDefinition(name = "Role")
        String role();
    }
}
