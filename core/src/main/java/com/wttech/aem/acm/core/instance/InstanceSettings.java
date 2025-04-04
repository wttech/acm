package com.wttech.aem.acm.core.instance;

import java.io.Serializable;

public class InstanceSettings implements Serializable {
    private String timezoneId;
    private boolean publish;
    private boolean cloud;

    public InstanceSettings(String timezoneId, boolean publish, boolean cloud) {
        this.timezoneId = timezoneId;
        this.cloud = cloud;
        this.publish = publish;
    }

    public String getTimezoneId() {
        return timezoneId;
    }

    public boolean isPublish() {
        return this.publish;
    }

    public boolean isCloud() {
        return this.cloud;
    }
}
