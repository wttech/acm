package dev.vml.es.acm.core.notification.teams;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import java.io.Serializable;
import java.util.*;

/**
 * Microsoft Teams Incoming Webhook payload using modern Adaptive Cards format.
 * 
 * @see <a href="https://docs.microsoft.com/en-us/microsoftteams/platform/webhooks-and-connectors/how-to/add-incoming-webhook">Teams Webhooks</a>
 * @see <a href="https://adaptivecards.io/explorer/">Adaptive Cards Explorer</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class TeamsPayload implements Serializable {

    private final String type = "message";

    private final List<Attachment> attachments;

    private TeamsPayload(List<Attachment> attachments) {
        this.attachments = attachments != null ? new ArrayList<>(attachments) : Collections.emptyList();
    }

    public String getType() {
        return type;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final List<CardElement> body = new ArrayList<>();
        private final List<Action> actions = new ArrayList<>();

        public Builder text(String text) {
            if (StringUtils.isNotBlank(text)) {
                body.add(TextBlock.create(text));
            }
            return this;
        }

        public Builder title(String title) {
            if (StringUtils.isNotBlank(title)) {
                body.add(0, TextBlock.create(title).size("Large").weight("Bolder"));
            }
            return this;
        }

        public Builder textBlock(String text) {
            if (StringUtils.isNotBlank(text)) {
                body.add(TextBlock.create(text));
            }
            return this;
        }

        public Builder facts(String... keyValuePairs) {
            if (keyValuePairs != null && keyValuePairs.length >= 2) {
                List<Fact> facts = new ArrayList<>();
                for (int i = 0; i < keyValuePairs.length - 1; i += 2) {
                    facts.add(Fact.create(keyValuePairs[i], keyValuePairs[i + 1]));
                }
                body.add(FactSet.create(facts));
            }
            return this;
        }

        public Builder openUrlAction(String title, String url) {
            if (StringUtils.isNotBlank(title) && StringUtils.isNotBlank(url)) {
                actions.add(Action.openUrl(title, url));
            }
            return this;
        }

        public TeamsPayload build() {
            AdaptiveCard card = AdaptiveCard.create(body, actions);
            Attachment attachment = Attachment.create(card);
            return new TeamsPayload(Collections.singletonList(attachment));
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Attachment implements Serializable {

        private final String contentType = "application/vnd.microsoft.card.adaptive";

        private final AdaptiveCard content;

        private Attachment(AdaptiveCard content) {
            this.content = content;
        }

        public static Attachment create(AdaptiveCard content) {
            return new Attachment(content);
        }

        public String getContentType() {
            return contentType;
        }

        public AdaptiveCard getContent() {
            return content;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class AdaptiveCard implements Serializable {

        private final String type = "AdaptiveCard";

        @JsonProperty("$schema")
        private final String schema = "http://adaptivecards.io/schemas/adaptive-card.json";

        private final String version = "1.2";

        private final List<CardElement> body;

        private final List<Action> actions;

        private AdaptiveCard(List<CardElement> body, List<Action> actions) {
            this.body = body != null ? new ArrayList<>(body) : Collections.emptyList();
            this.actions = actions != null && !actions.isEmpty() ? new ArrayList<>(actions) : null;
        }

        public static AdaptiveCard create(List<CardElement> body, List<Action> actions) {
            return new AdaptiveCard(body, actions);
        }

        public String getType() {
            return type;
        }

        public String getSchema() {
            return schema;
        }

        public String getVersion() {
            return version;
        }

        public List<CardElement> getBody() {
            return body;
        }

        public List<Action> getActions() {
            return actions;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class TextBlock implements CardElement, Serializable {

        private final String type = "TextBlock";

        private final String text;

        private String size;

        private String weight;

        private Boolean wrap;

        private TextBlock(String text) {
            this.text = StringUtils.defaultString(text);
        }

        public static TextBlock create(String text) {
            return new TextBlock(text);
        }

        public TextBlock size(String size) {
            this.size = size;
            return this;
        }

        public TextBlock weight(String weight) {
            this.weight = weight;
            return this;
        }

        public TextBlock wrap(boolean wrap) {
            this.wrap = wrap;
            return this;
        }

        public String getType() {
            return type;
        }

        public String getText() {
            return text;
        }

        public String getSize() {
            return size;
        }

        public String getWeight() {
            return weight;
        }

        public Boolean getWrap() {
            return wrap;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class FactSet implements CardElement, Serializable {

        private final String type = "FactSet";

        private final List<Fact> facts;

        private FactSet(List<Fact> facts) {
            this.facts = facts != null ? new ArrayList<>(facts) : Collections.emptyList();
        }

        public static FactSet create(List<Fact> facts) {
            return new FactSet(facts);
        }

        public String getType() {
            return type;
        }

        public List<Fact> getFacts() {
            return facts;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Fact implements Serializable {

        private final String title;

        private final String value;

        private Fact(String title, String value) {
            this.title = StringUtils.defaultString(title);
            this.value = StringUtils.defaultString(value);
        }

        public static Fact create(String title, String value) {
            return new Fact(title, value);
        }

        public String getTitle() {
            return title;
        }

        public String getValue() {
            return value;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Action implements Serializable {

        private final String type;

        private final String title;

        private final String url;

        private Action(String type, String title, String url) {
            this.type = type;
            this.title = StringUtils.defaultString(title);
            this.url = url;
        }

        public static Action openUrl(String title, String url) {
            return new Action("Action.OpenUrl", title, url);
        }

        public String getType() {
            return type;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }
    }

    public interface CardElement {
        // marker interface for card elements
    }
}
