package dev.vml.es.acm.core.notification.teams;

import dev.vml.es.acm.core.notification.Notifier;
import dev.vml.es.acm.core.notification.slack.SlackException;
import dev.vml.es.acm.core.util.JsonUtils;
import java.io.IOException;

public class Teams implements Notifier<TeamsPayload> {

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
    public void sendPayload(TeamsPayload payload) {
        try {
            sendPayload(JsonUtils.writeToString(payload));
        } catch (IOException e) {
            throw new SlackException(String.format("Cannot serialize Teams payload for notifier '%s'!", id), e);
        }
    }

    @Override
    public void sendPayload(String payload) {
        // TODO ...
    }

    @Override
    public void close() throws IOException {
        // TODO ...
    }
}
