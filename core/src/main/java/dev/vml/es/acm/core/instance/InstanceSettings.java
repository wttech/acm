package dev.vml.es.acm.core.instance;

import dev.vml.es.acm.core.osgi.InstanceInfo;
import dev.vml.es.acm.core.osgi.InstanceRole;
import dev.vml.es.acm.core.osgi.InstanceType;
import dev.vml.es.acm.core.util.DateUtils;
import java.io.Serializable;

public class InstanceSettings implements Serializable {

    private final String timezoneId;

    private final InstanceRole role;

    private final InstanceType type;

    public InstanceSettings(InstanceInfo instanceInfo) {
        this.timezoneId = DateUtils.TIMEZONE_ID;
        this.role = instanceInfo.getRole();
        this.type = instanceInfo.getType();
    }

    public String getTimezoneId() {
        return timezoneId;
    }

    public InstanceRole getRole() {
        return role;
    }

    public InstanceType getType() {
        return type;
    }
}
