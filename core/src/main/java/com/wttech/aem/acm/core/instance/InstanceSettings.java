package com.wttech.aem.acm.core.instance;

import java.io.Serializable;

public class InstanceSettings implements Serializable {
    private String timezoneId;
    private boolean publish;
    private boolean cloudVersion;

    public InstanceSettings(String timezoneId, boolean publish, boolean cloudVersion) {
        this.timezoneId = timezoneId;
        this.cloudVersion = cloudVersion;
        this.publish = publish;
    }

    public String getTimezoneId() {
        return timezoneId;
    }

    public boolean isPublish() {
        return this.publish;
    }

    public boolean isCloudVersion() {
        return this.cloudVersion;
    }
}
