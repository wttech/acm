package com.vml.es.aem.acm.core.mock;

import java.io.Serializable;

public class MockStatus implements Serializable {

    private final boolean enabled;

    public MockStatus(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
