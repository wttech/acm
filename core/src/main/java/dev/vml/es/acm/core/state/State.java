package dev.vml.es.acm.core.state;

import dev.vml.es.acm.core.gui.SpaSettings;
import dev.vml.es.acm.core.instance.HealthStatus;
import dev.vml.es.acm.core.instance.InstanceSettings;
import dev.vml.es.acm.core.mock.MockStatus;
import java.io.Serializable;

public class State implements Serializable {

    private final HealthStatus healthStatus;

    private final MockStatus mockStatus;

    private final InstanceSettings instanceSettings;

    private final SpaSettings spaSettings;

    private final Permissions permissions;

    public State(
            SpaSettings spaSettings,
            HealthStatus healthStatus,
            MockStatus mockStatus,
            InstanceSettings instanceSettings,
            Permissions permissions) {
        this.spaSettings = spaSettings;
        this.healthStatus = healthStatus;
        this.mockStatus = mockStatus;
        this.instanceSettings = instanceSettings;
        this.permissions = permissions;
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

    public SpaSettings getSpaSettings() {
        return spaSettings;
    }

    public Permissions getPermissions() {
        return permissions;
    }
}
