package dev.vml.es.acm.core.code;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.util.YamlUtils;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutableMetadata implements Serializable {

    public static final ExecutableMetadata EMPTY = new ExecutableMetadata(new LinkedHashMap<>());

    private static final Logger LOG = LoggerFactory.getLogger(ExecutableMetadata.class);

    private static final String COMMENT_START = "/*";

    private static final String COMMENT_END = "*/";

    private static final String JAVADOC_COMMENT_START = "/**";

    private static final String FRONTMATTER_DELIMITER = "---";

    private Map<String, Object> values;

    public ExecutableMetadata(Map<String, Object> values) {
        this.values = values;
    }

    public static ExecutableMetadata of(Executable executable) {
        try {
            return parse(executable.getContent());
        } catch (Exception e) {
            LOG.warn("Cannot parse code metadata from executable '{}'!", executable.getId(), e);
            return EMPTY;
        }
    }

    public static ExecutableMetadata parse(String code) {
        if (StringUtils.isNotBlank(code)) {
            String blockComment = findFirstBlockComment(code);
            if (blockComment != null) {
                return new ExecutableMetadata(parseBlockComment(blockComment));
            }
        }
        return EMPTY;
    }

    /**
     * Finds first block comment that's properly separated with blank lines.
     * Must be followed by a blank line (not directly attached to code).
     * Can appear at the start of the file or after import/package statements.
     */
    private static String findFirstBlockComment(String code) {
        int commentStart = code.indexOf(COMMENT_START);
        while (commentStart >= 0) {
            int closingMarker = code.indexOf(COMMENT_END, commentStart + COMMENT_START.length());
            if (closingMarker < 0) {
                break;
            }

            // Resume the next search after this comment's closing marker (not just past its
            // opening marker) so that text already consumed as part of this comment's body
            // (e.g. a literal "/*" sequence inside it) is never re-considered as a separate,
            // independent comment.
            int commentEnd = closingMarker + COMMENT_END.length();
            boolean isJavadoc = code.startsWith(JAVADOC_COMMENT_START, commentStart);

            if (!isJavadoc) {
                String afterComment = code.substring(commentEnd);
                if (hasWhitespaceLines(afterComment, 2)) {
                    if (commentStart == 0) {
                        return code.substring(commentStart, commentEnd);
                    }
                    String beforeComment = code.substring(0, commentStart);
                    if (beforeComment.trim().isEmpty() || isAfterImportOrPackage(beforeComment)) {
                        return code.substring(commentStart, commentEnd);
                    }
                }
            }

            commentStart = code.indexOf(COMMENT_START, commentEnd);
        }

        return null;
    }

    private static boolean hasWhitespaceLines(String value, int requiredLines) {
        int lineCount = 0;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '\n') {
                lineCount++;
                if (lineCount >= requiredLines) {
                    return true;
                }
            } else if (!Character.isWhitespace(character)) {
                return false;
            }
        }
        return false;
    }

    private static boolean isAfterImportOrPackage(String value) {
        return endsWithWhitespaceLines(value, 2) && (value.contains("import") || value.contains("package"));
    }

    private static boolean endsWithWhitespaceLines(String value, int requiredLines) {
        int lineCount = 0;
        for (int index = value.length() - 1; index >= 0; index--) {
            char character = value.charAt(index);
            if (character == '\n') {
                lineCount++;
                if (lineCount >= requiredLines) {
                    return true;
                }
            } else if (!Character.isWhitespace(character)) {
                return false;
            }
        }
        return false;
    }

    /**
     * Extracts frontmatter (YAML between triple dashes) and description from block comment.
     */
    private static Map<String, Object> parseBlockComment(String blockComment) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (StringUtils.isBlank(blockComment)) {
            return result;
        }

        String content = blockComment
                .substring(COMMENT_START.length(), blockComment.length() - COMMENT_END.length())
                .trim();

        String description = content;
        int frontmatterStart = content.startsWith(FRONTMATTER_DELIMITER) ? content.indexOf('\n') : -1;
        int[] closingDelimiter = findClosingDelimiter(content, frontmatterStart);

        if (closingDelimiter != null) {
            String frontmatter = content.substring(frontmatterStart + 1, closingDelimiter[0]);
            result.putAll(parseFrontmatter(frontmatter));
            description = content.substring(closingDelimiter[1]);
        }

        description = description.trim();

        if (!description.isEmpty()) {
            result.put("description", description);
        }

        return result;
    }

    /**
     * Finds the closing "---" delimiter line, returning its start offset (exclusive end of the
     * frontmatter body) and the offset right after it (start of the description), or {@code null}
     * if no closing delimiter exists.
     */
    private static int[] findClosingDelimiter(String content, int openingLineEnd) {
        if (openingLineEnd < 0 || !isWhitespace(content, FRONTMATTER_DELIMITER.length(), openingLineEnd)) {
            return null;
        }

        int lineStart = openingLineEnd + 1;
        while (lineStart < content.length()) {
            int lineEnd = content.indexOf('\n', lineStart);
            if (lineEnd < 0) {
                return null;
            }
            if (lineStart > openingLineEnd + 1
                    && content.startsWith(FRONTMATTER_DELIMITER, lineStart)
                    && isWhitespace(content, lineStart + FRONTMATTER_DELIMITER.length(), lineEnd)) {
                return new int[] {lineStart, lineEnd + 1};
            }
            lineStart = lineEnd + 1;
        }
        return null;
    }

    private static boolean isWhitespace(String value, int start, int end) {
        for (int index = start; index < end; index++) {
            if (!Character.isWhitespace(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, Object> parseFrontmatter(String frontmatter) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> yamlData = YamlUtils.readFromString(frontmatter, Map.class);
            return yamlData != null ? yamlData : new LinkedHashMap<>();
        } catch (Exception e) {
            throw new AcmException(String.format("Cannot parse frontmatter!\n%s\n", frontmatter), e);
        }
    }

    @JsonAnyGetter
    public Map<String, Object> getValues() {
        return values;
    }
}
