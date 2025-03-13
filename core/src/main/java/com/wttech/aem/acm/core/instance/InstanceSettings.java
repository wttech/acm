package com.wttech.aem.acm.core.instance;

import java.io.Serializable;
import java.util.TimeZone;

public class InstanceSettings implements Serializable {

    private String timezoneId;

    public InstanceSettings(String timezoneId) {
        this.timezoneId = timezoneId;
    }

    public static InstanceSettings current() {
        return new InstanceSettings(TimeZone.getDefault().getID());
    }

    public String getTimezoneId() {
        return timezoneId;
    }
}
