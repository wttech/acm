package dev.vml.es.acm.core.notification.slack;

import dev.vml.es.acm.core.notification.Notifier;
import dev.vml.es.acm.core.util.JsonUtils;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slack implements Notifier<SlackPayload> {

    private static final Logger LOG = LoggerFactory.getLogger(Slack.class);

    private static final int[] STATUS_CODE_ACCEPTED = {200, 202};

    private final String id;

    private final String webhookUrl;

    private final boolean enabled;

    private final CloseableHttpClient httpClient;

    private final RequestConfig httpRequestConfig;

    public Slack(String id, String webhookUrl, boolean enabled, int timeoutMillis) {
        this.id = id;
        this.webhookUrl = webhookUrl;
        this.enabled = enabled;
        this.httpRequestConfig = RequestConfig.custom()
                .setConnectTimeout(timeoutMillis)
                .setConnectionRequestTimeout(timeoutMillis)
                .setSocketTimeout(timeoutMillis)
                .build();
        this.httpClient =
                HttpClients.custom().setDefaultRequestConfig(httpRequestConfig).build();
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
    public void sendMessage(String title, String text, Map<String, Object> fields) {
        SlackPayload payload =
                SlackPayload.builder().message(title, text, fields).build();
        sendPayload(payload);
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
        if (!enabled) {
            LOG.debug("Slack notifier '{}' disabled. Skipping sending payload: {}", id, payloadJson);
            return;
        }

        HttpPost post = new HttpPost(webhookUrl);
        post.setConfig(httpRequestConfig);
        post.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        post.setEntity(new StringEntity(payloadJson, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int status = response.getStatusLine().getStatusCode();

            if (ArrayUtils.contains(STATUS_CODE_ACCEPTED, status)) {
                LOG.debug("Slack notifier '{}' sent payload to webhook with success (status={})", id, status);
            } else {
                String body = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "<empty>";
                String statusLine = response.getStatusLine().toString();
                throw new SlackException(
                        String.format("Slack webhook error for notifier '%s' (%s): %s", id, statusLine, body));
            }
        } catch (Exception e) {
            throw new SlackException(String.format("Cannot send Slack payload for notifier '%s'!", id), e);
        }
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }
}
