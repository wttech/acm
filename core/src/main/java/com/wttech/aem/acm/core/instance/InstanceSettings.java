package com.wttech.aem.acm.core.instance;

import com.wttech.aem.acm.core.osgi.InstanceInfo;
import java.io.Serializable;
import java.util.TimeZone;

public class InstanceSettings implements Serializable {

    private final String timezoneId;

    private final boolean author;

    private final boolean publish;

    private final boolean onPrem;

    private final boolean cloud;

    private final boolean cloudContainer;

    private final boolean cloudSdk;

    public InstanceSettings(InstanceInfo instanceInfo) {
        this.timezoneId = TimeZone.getDefault().getID();
        this.author = instanceInfo.isAuthor();
        this.publish = instanceInfo.isPublish();
        this.onPrem = instanceInfo.isOnPrem();
        this.cloud = instanceInfo.isCloud();
        this.cloudContainer = instanceInfo.isCloudContainer();
        this.cloudSdk = instanceInfo.isCloudSdk();
    }

    public String getTimezoneId() {
        return timezoneId;
    }

    public boolean isAuthor() {
        return author;
    }

    public boolean isPublish() {
        return this.publish;
    }

    public boolean isCloud() {
        return this.cloud;
    }

    public boolean isCloudSdk() {
        return cloudSdk;
    }

    public boolean isCloudContainer() {
        return cloudContainer;
    }
}
