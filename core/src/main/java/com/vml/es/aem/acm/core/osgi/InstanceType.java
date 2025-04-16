package com.vml.es.aem.acm.core.osgi;

import java.util.Arrays;
import java.util.Optional;

public enum InstanceType {
    ON_PREM,
    CLOUD_SDK,
    CLOUD_CONTAINER;

    public static Optional<InstanceType> of(String type) {
        return Arrays.stream(values())
                .filter(t -> t.name().equalsIgnoreCase(type))
                .findFirst();
    }

    public boolean isCloud() {
        return this == CLOUD_SDK || this == CLOUD_CONTAINER;
    }
}
