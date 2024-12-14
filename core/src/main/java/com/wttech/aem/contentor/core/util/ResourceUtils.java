package com.wttech.aem.contentor.core.util;

import java.util.HashMap;
import java.util.Map;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

public final class ResourceUtils {

  private ResourceUtils() {
    // intentionally empty
  }

  public static ResourceResolver serviceResolver(ResourceResolverFactory resourceResolverFactory)
      throws LoginException {
    Map<String, Object> serviceParams = new HashMap<>();
    serviceParams.put(ResourceResolverFactory.SUBSERVICE, "contentor");
    return resourceResolverFactory.getServiceResourceResolver(serviceParams);
  }
}
