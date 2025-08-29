package dev.vml.es.acm.core.notification.teams;

import dev.vml.es.acm.core.notification.Notifier;
import java.io.IOException;

public class Teams implements Notifier<TeamsMessage> {

    private final String id;

    private final String webhookUrl;

    private final boolean enabled;

    public Teams(String id, String webhookUrl, boolean enabled) {
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
    public void sendMessage(TeamsMessage message) {
        // TODO ...
    }

    @Override
    public void close() throws IOException {
        // TODO ...
    }
}
