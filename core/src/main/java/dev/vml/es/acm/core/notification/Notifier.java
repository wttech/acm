package dev.vml.es.acm.core.notification;

import java.io.Closeable;
import java.io.Serializable;
import java.util.Map;

public interface Notifier<P extends Serializable> extends Closeable {

    /**
     * Unique identifier of this notifier.
     * As notifier usually used webhooks which are tied to a specific channel or group, this ID can be used to identify the target.
     */
    String getId();

    /**
     * Check if this notifier is enabled.
     * When disabled, only logs are written, but no notifications are sent.
     */
    boolean isEnabled();

    /**
     * Send a payload to notification service in raw format.
     */
    void sendPayload(String payload);

    /**
     * Send a payload to notification service in structured format.
     */
    void sendPayload(P payload);

    /**
     * Send a message to notification service in structured format.
     */
    void sendMessage(String title, String text, Map<String, Object> fields);
}
