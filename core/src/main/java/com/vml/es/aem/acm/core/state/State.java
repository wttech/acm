package com.vml.es.aem.acm.core.state;

import com.vml.es.aem.acm.core.code.ExecutionSummary;
import com.vml.es.aem.acm.core.gui.SpaSettings;
import com.vml.es.aem.acm.core.instance.HealthStatus;
import com.vml.es.aem.acm.core.instance.InstanceSettings;
import java.io.Serializable;
import java.util.List;

public class State implements Serializable {

    private final HealthStatus healthStatus;

    private final MockStatus mockStatus;

    private final InstanceSettings instanceSettings;

    private final List<ExecutionSummary> queuedExecutions;

    private final SpaSettings spaSettings;

    public State(
            SpaSettings spaSettings,
            HealthStatus healthStatus,
            MockStatus mockStatus,
            InstanceSettings instanceSettings,
            List<ExecutionSummary> queuedExecutions) {
        this.spaSettings = spaSettings;
        this.healthStatus = healthStatus;
        this.mockStatus = mockStatus;
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

    public SpaSettings getSpaSettings() {
        return spaSettings;
    }
}
