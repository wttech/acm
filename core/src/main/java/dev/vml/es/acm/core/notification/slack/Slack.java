package dev.vml.es.acm.core.notification.slack;

import dev.vml.es.acm.core.notification.Notifier;
import java.io.IOException;

public class Slack implements Notifier<SlackMessage> {

    private final String id;

    private final String webhookUrl;

    private final boolean enabled;

    public Slack(String id, String webhookUrl, boolean enabled) {
        this.id = id;
        this.webhookUrl = webhookUrl;
        this.enabled = enabled;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void sendMessage(SlackMessage message) {
        // TODO ...
    }

    @Override
    public void close() throws IOException {
        // TODO ...
    }
}
