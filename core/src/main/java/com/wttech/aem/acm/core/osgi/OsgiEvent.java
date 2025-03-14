package com.wttech.aem.acm.core.osgi;

public class OsgiEvent {

    private final String topic;

    private final long received;

    public OsgiEvent(String topic, long received) {
        this.topic = topic;
        this.received = received;
    }

    public String getTopic() {
        return topic;
    }

    public long getReceived() {
        return received;
    }
}
