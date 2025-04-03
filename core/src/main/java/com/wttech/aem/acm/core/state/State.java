package com.wttech.aem.acm.core.state;

import com.wttech.aem.acm.core.code.ExecutionSummary;
import com.wttech.aem.acm.core.instance.HealthStatus;
import com.wttech.aem.acm.core.instance.InstanceSettings;
import java.io.Serializable;
import java.util.List;

public class State implements Serializable {

    private final HealthStatus healthStatus;

    private final InstanceSettings instanceSettings;

    private final List<ExecutionSummary> queuedExecutions;

    public State(
            HealthStatus healthStatus, InstanceSettings instanceSettings, List<ExecutionSummary> queuedExecutions) {
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

    public List<ExecutionSummary> getQueuedExecutions() {
        return queuedExecutions;
    }
}
