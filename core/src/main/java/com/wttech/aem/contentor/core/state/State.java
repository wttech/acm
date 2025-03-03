package com.wttech.aem.contentor.core.state;

import com.wttech.aem.contentor.core.instance.HealthStatus;
import com.wttech.aem.contentor.core.instance.InstanceSettings;
import java.io.Serializable;

public class State implements Serializable {

    private HealthStatus healthStatus;

    private InstanceSettings instanceSettings;

    public State(HealthStatus healthStatus, InstanceSettings instanceSettings) {
        this.healthStatus = healthStatus;
        this.instanceSettings = instanceSettings;
    }

    public HealthStatus getHealthStatus() {
        return healthStatus;
    }

    public InstanceSettings getInstanceSettings() {
        return instanceSettings;
    }
}
