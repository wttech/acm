package com.wttech.aem.contentor.core.instance;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(immediate = true, service = HealthChecker.class)
@Designate(ocd = HealthChecker.Config.class)
public class HealthChecker {

  private Config config;

  @Activate
  @Modified
  protected void activate(Config config) {
    this.config = config;
  }

  public boolean isHealthy() {
    return true; // TODO implement this like in:
    // https://github.com/wttech/aemc/blob/main/pkg/instance_manager_check.go
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
