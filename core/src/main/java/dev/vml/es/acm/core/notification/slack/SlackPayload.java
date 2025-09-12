package dev.vml.es.acm.core.notification.slack;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import dev.vml.es.acm.core.util.StringUtil;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * Minimal JSON-level Slack Incoming Webhook payload.
 * Blocks are generic maps, so new Slack fields/types need no new Java classes.
 *
 * @see <a href="https://github.com/slackapi/java-slack-sdk/blob/main/slack-api-model/src/main/java/com/slack/api/webhook/Payload.java">Slack Webhook Payload</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SlackPayload implements Serializable {

    private final String text;

    private final List<Block> blocks;

    private SlackPayload(String text, List<Block> blocks) {
        this.text = text;
        this.blocks = blocks;
    }

    public String getText() {
        return text;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String text;

        private final List<Block> blocks = new ArrayList<>();

        public Builder message(String title, String text, Map<String, Object> fields) {
            Builder payload = new Builder();
            if (StringUtils.isNotBlank(title)) {
                payload.header(title);
            }
            if (StringUtils.isNotBlank(title) && StringUtils.isNotBlank(text)) {
                payload.divider();
            }
            if (StringUtils.isNotBlank(text)) {
                payload.sectionMarkdown(text);
            }
            if (fields != null && !fields.isEmpty()) {
                payload.fieldsMarkdown(fields);
            }
            return payload;
        }

        public Builder text(String text) {
            this.text = StringUtils.defaultString(text);
            return this;
        }

        public Builder add(Block block) {
            blocks.add(block);
            return this;
        }

        public Builder header(String plain) {
            return add(Block.header(plain));
        }

        public Builder sectionMarkdown(String md) {
            return add(Block.sectionMarkdown(md));
        }

        public Builder sectionPlain(String plain) {
            return add(Block.sectionPlain(plain));
        }

        public Builder divider() {
            return add(Block.divider());
        }

        public Builder fieldsMarkdown(String... mdFields) {
            return fieldsMarkdown(Arrays.asList(mdFields));
        }

        public Builder fieldsMarkdown(List<String> mdFields) {
            return fieldsMarkdown((Collection<String>) mdFields);
        }

        public Builder fieldsMarkdown(Map<String, Object> fields) {
            List<String> fieldTexts = new ArrayList<>();
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                String key = StringUtil.toStringOrEmpty(entry.getKey());
                String value = StringUtil.toStringOrEmpty(entry.getValue());
                fieldTexts.add("*" + key + "*\n" + value);
            }
            return fieldsMarkdown(fieldTexts);
        }

        public Builder fieldsMarkdown(Collection<String> mdFields) {
            List<String> safeFields =
                    mdFields.stream().map(StringUtils::defaultString).collect(Collectors.toList());
            return add(Block.fieldsSectionMarkdown(safeFields));
        }

        public SlackPayload build() {
            return new SlackPayload(text, blocks);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Block implements Serializable {

        private final Map<String, Object> data = new LinkedHashMap<>();

        private Block(String type) {
            data.put("type", type);
        }

        public static Block raw(Map<String, Object> raw) {
            Block b = new Block(raw == null ? "unknown" : String.valueOf(raw.get("type")));
            b.data.clear();
            if (raw != null) {
                b.data.putAll(raw);
            }
            return b;
        }

        public static Block header(String plain) {
            return new Block("header").with("text", plainText(plain, true));
        }

        public static Block sectionMarkdown(String md) {
            return new Block("section").with("text", markdown(md));
        }

        public static Block sectionPlain(String plain) {
            return new Block("section").with("text", plainText(plain, true));
        }

        public static Block fieldsSectionMarkdown(List<String> markdownFields) {
            Block b = new Block("section");
            if (markdownFields != null && !markdownFields.isEmpty()) {
                List<Map<String, Object>> fields = new ArrayList<>();
                for (String f : markdownFields) {
                    fields.add(markdown(f));
                }
                b.with("fields", fields);
            }
            return b;
        }

        public static Block divider() {
            return new Block("divider");
        }

        public Block with(String key, Object value) {
            if (value == null) {
                data.remove(key);
            } else {
                data.put(key, value);
            }
            return this;
        }

        public String getType() {
            Object t = data.get("type");
            return t == null ? null : t.toString();
        }

        @JsonAnyGetter
        Map<String, Object> any() {
            return Collections.unmodifiableMap(data);
        }
    }

    private static Map<String, Object> markdown(String text) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "mrkdwn");
        m.put("text", StringUtils.defaultString(text));
        return m;
    }

    private static Map<String, Object> plainText(String text, boolean emoji) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "plain_text");
        m.put("text", StringUtils.defaultString(text));
        m.put("emoji", emoji);
        return m;
    }
}
