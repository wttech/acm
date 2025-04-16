package com.vml.es.aem.acm.core.instance;

import com.vml.es.aem.acm.core.osgi.InstanceInfo;
import com.vml.es.aem.acm.core.osgi.InstanceRole;
import com.vml.es.aem.acm.core.osgi.InstanceType;
import java.io.Serializable;
import java.util.TimeZone;

public class InstanceSettings implements Serializable {

    private final String timezoneId;

    private final InstanceRole role;

    private final InstanceType type;

    public InstanceSettings(InstanceInfo instanceInfo) {
        this.timezoneId = TimeZone.getDefault().getID();
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
