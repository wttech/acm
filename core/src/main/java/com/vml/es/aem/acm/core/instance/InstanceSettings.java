package com.vml.es.aem.acm.core.instance;

import com.vml.es.aem.acm.core.osgi.InstanceInfo;
import com.vml.es.aem.acm.core.osgi.InstanceRole;
import com.vml.es.aem.acm.core.osgi.InstanceType;
import com.vml.es.aem.acm.core.util.DateUtils;
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
