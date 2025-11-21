package dev.vml.es.acm.core.code;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodeMetadata implements Serializable {

    public static final CodeMetadata EMPTY = new CodeMetadata(new LinkedHashMap<>());

    private static final Logger LOG = LoggerFactory.getLogger(CodeMetadata.class);

    private static final Pattern DOC_COMMENT_PATTERN = Pattern.compile("/\\*\\*([^*]|\\*(?!/))*\\*/", Pattern.DOTALL);

    private static final Pattern TAG_PATTERN =
            Pattern.compile("(?m)^\\s*\\*?\\s*@(\\w+)\\s+(.+?)(?=(?m)^\\s*\\*?\\s*@\\w+|\\*/|$)", Pattern.DOTALL);

    private Map<String, Object> values;

    public CodeMetadata(Map<String, Object> values) {
        this.values = values;
    }

    public static CodeMetadata of(Executable executable) {
        try {
            return parse(executable.getContent());
        } catch (Exception e) {
            LOG.warn("Cannot parse code metadata from executable '{}'!", executable.getId(), e);
            return EMPTY;
        }
    }

    public static CodeMetadata parse(String code) {
        if (StringUtils.isNotBlank(code)) {
            String docComment = findFirstDocComment(code);
            if (docComment != null) {
                return new CodeMetadata(parseDocComment(docComment));
            }
        }
        return EMPTY;
    }

    /**
     * Find the first doc comment (either at the top or after imports)
     */
    private static String findFirstDocComment(String code) {
        Matcher matcher = DOC_COMMENT_PATTERN.matcher(code);

        // Find first doc comment that's properly separated with newlines
        while (matcher.find()) {
            String comment = matcher.group();
            int commentStart = matcher.start();
            int commentEnd = matcher.end();

            // Get text after comment (until next non-whitespace or end of line)
            String afterComment = code.substring(commentEnd);

            // Must have at least one newline after the comment
            if (!afterComment.matches("^\\s*\\n[\\s\\S]*")) {
                continue;
            }

            // Check what follows after the newline(s)
            String trimmedAfter = afterComment.trim();

            // Skip if directly followed by describeRun (it's OK to be attached)
            if (trimmedAfter.startsWith("void describeRun()")) {
                return comment;
            }

            // Must have blank line after (double newline) for other cases
            if (!afterComment.matches("^\\s*\\n\\s*\\n[\\s\\S]*")) {
                continue;
            }

            // If there's code before the comment, check for blank line before
            if (commentStart > 0) {
                String beforeComment = code.substring(0, commentStart);
                // Should have imports or package, followed by blank line
                if (beforeComment.trim().isEmpty()
                        || beforeComment.matches("[\\s\\S]*(import|package)[\\s\\S]*\\n\\s*\\n\\s*$")) {
                    return comment;
                }
            } else {
                // Comment at the very start is OK
                return comment;
            }
        }

        return null;
    }

    /**
     * Parses the doc comment to extract description and tags.
     */
    private static Map<String, Object> parseDocComment(String docComment) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Remove /** and */ markers and leading comment decorations
        String content = docComment.replaceAll("^/\\*\\*", "").replaceAll("\\*/$", "");

        // Extract general description (text before first @tag)
        // Look for @tag pattern (@ at start of word boundary, not in middle of text
        // like email)
        Pattern firstTagPattern = Pattern.compile("(?m)^\\s*\\*?\\s*@\\w+");
        Matcher firstTagMatcher = firstTagPattern.matcher(content);

        if (firstTagMatcher.find()) {
            int firstTagIndex = firstTagMatcher.start();
            String description = content.substring(0, firstTagIndex)
                    .replaceAll("(?m)^\\s*\\*\\s?", "")
                    .trim();
            if (!description.isEmpty()) {
                result.put("description", description);
            }
        } else {
            // No tags, just description
            String description = content.replaceAll("(?m)^\\s*\\*\\s?", "").trim();
            if (!description.isEmpty()) {
                result.put("description", description);
            }
        }

        // Parse tags
        Matcher tagMatcher = TAG_PATTERN.matcher(content);

        while (tagMatcher.find()) {
            String tagName = tagMatcher.group(1);
            String tagValue = tagMatcher.group(2);

            if (tagValue != null) {
                tagValue = tagValue.replaceAll("(?m)^\\s*\\*\\s?", "") // Remove leading * from each line
                        .trim();

                if (!tagValue.isEmpty()) {
                    // Store tag value, use list for potential multiple values
                    Object existing = result.get(tagName);

                    if (existing == null) {
                        result.put(tagName, tagValue);
                    } else if (existing instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> list = (List<String>) existing;
                        list.add(tagValue);
                    } else {
                        // Convert to list if we have multiple values
                        List<String> list = new ArrayList<>();
                        list.add((String) existing);
                        list.add(tagValue);
                        result.put(tagName, list);
                    }
                }
            }
        }
        return result;
    }

    @JsonAnyGetter
    public Map<String, Object> getValues() {
        return values;
    }
}
