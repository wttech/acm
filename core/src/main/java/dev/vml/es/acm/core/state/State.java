package dev.vml.es.acm.core.state;

import dev.vml.es.acm.core.code.ExecutionSummary;
import dev.vml.es.acm.core.gui.SpaSettings;
import dev.vml.es.acm.core.instance.HealthStatus;
import dev.vml.es.acm.core.instance.InstanceSettings;
import dev.vml.es.acm.core.mock.MockStatus;
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

    public MockStatus getMockStatus() {
        return mockStatus;
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
