package dev.vml.es.acm.core.notification.slack;

import dev.vml.es.acm.core.notification.Notifier;
import dev.vml.es.acm.core.util.JsonUtils;
import java.io.IOException;

public class Slack implements Notifier<SlackPayload> {

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
    public void sendPayload(SlackPayload payload) {
        try {
            sendPayload(JsonUtils.writeToString(payload));
        } catch (IOException e) {
            throw new SlackException(String.format("Cannot serialize Slack payload for notifier '%s'!", id), e);
        }
    }

    @Override
    public void sendPayload(String payloadJson) {
        // TODO ...
    }

    @Override
    public void close() throws IOException {
        // TODO ...
    }
}
