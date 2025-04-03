package com.wttech.aem.acm.core.state;

import com.wttech.aem.acm.core.code.Execution;
import com.wttech.aem.acm.core.instance.HealthStatus;
import com.wttech.aem.acm.core.instance.InstanceSettings;
import java.io.Serializable;
import java.util.List;

public class State implements Serializable {

    private HealthStatus healthStatus;

    private InstanceSettings instanceSettings;

    private List<Execution> queuedExecutions;

    public State(HealthStatus healthStatus, InstanceSettings instanceSettings, List<Execution> queuedExecutions) {
        this.healthStatus = healthStatus;
        this.instanceSettings = instanceSettings;
        this.queuedExecutions = queuedExecutions;
    }

    public HealthStatus getHealthStatus() {
        return healthStatus;
    }

    public InstanceSettings getInstanceSettings() {
        return instanceSettings;
    }

    public List<Execution> getQueuedExecutions() {
        return queuedExecutions;
    }
}
