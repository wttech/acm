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
    private static final Pattern NEWLINE_AFTER_COMMENT = Pattern.compile("^\\s*\\n[\\s\\S]*");
    private static final Pattern BLANK_LINE_AFTER_COMMENT = Pattern.compile("^\\s*\\n\\s*\\n[\\s\\S]*");
    private static final Pattern IMPORT_OR_PACKAGE_BEFORE = Pattern.compile("[\\s\\S]*(import|package)[\\s\\S]*\\n\\s*\\n\\s*$");
    private static final Pattern FIRST_TAG_PATTERN = Pattern.compile("(?m)^\\s*\\*?\\s*@\\w+");
    private static final Pattern LEADING_ASTERISK = Pattern.compile("(?m)^\\s*\\*\\s?");
    private static final Pattern DOC_MARKERS = Pattern.compile("^/\\*\\*|\\*/$");

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
            if (!NEWLINE_AFTER_COMMENT.matcher(afterComment).matches()) {
                continue;
            }

            // Check what follows after the newline(s)
            String trimmedAfter = afterComment.trim();

            // Skip if directly followed by describeRun (it's OK to be attached)
            if (trimmedAfter.startsWith("void describeRun()")) {
                return comment;
            }

            // Must have blank line after (double newline) for other cases
            if (!BLANK_LINE_AFTER_COMMENT.matcher(afterComment).matches()) {
                continue;
            }

            // If there's code before the comment, check for blank line before
            if (commentStart > 0) {
                String beforeComment = code.substring(0, commentStart);
                // Should have imports or package, followed by blank line
                if (beforeComment.trim().isEmpty()
                        || IMPORT_OR_PACKAGE_BEFORE.matcher(beforeComment).matches()) {
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
        String content = DOC_MARKERS.matcher(docComment).replaceAll("");

        // Extract general description (text before first @tag)
        // Look for @tag pattern (@ at start of word boundary, not in middle of text
        // like email)
        Matcher firstTagMatcher = FIRST_TAG_PATTERN.matcher(content);

        if (firstTagMatcher.find()) {
            int firstTagIndex = firstTagMatcher.start();
            String description = LEADING_ASTERISK.matcher(content.substring(0, firstTagIndex))
                    .replaceAll("")
                    .trim();
            if (!description.isEmpty()) {
                result.put("description", description);
            }
        } else {
            // No tags, just description
            String description = LEADING_ASTERISK.matcher(content).replaceAll("").trim();
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
                tagValue = LEADING_ASTERISK.matcher(tagValue).replaceAll("") // Remove leading * from each line
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
