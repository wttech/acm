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
        int commentStart = code.indexOf("/*");
        while (commentStart >= 0) {
            if (commentStart + 2 < code.length() && code.charAt(commentStart + 2) == '*') {
                commentStart = code.indexOf("/*", commentStart + 2);
                continue;
            }

            int closingMarker = code.indexOf("*/", commentStart + 2);
            if (closingMarker < 0) {
                break;
            }

            int commentEnd = closingMarker + 2;
            String comment = code.substring(commentStart, commentEnd);

            String afterComment = code.substring(commentEnd);

            if (!hasWhitespaceLines(afterComment, 1)) {
                continue;
            }

            if (!hasWhitespaceLines(afterComment, 2)) {
                continue;
            }

            if (commentStart > 0) {
                String beforeComment = code.substring(0, commentStart);
                String trimmedBefore = beforeComment.trim();
                if (trimmedBefore.isEmpty() || isAfterImportOrPackage(beforeComment)) {
                    return comment;
                }
            } else {
                return comment;
            }

            commentStart = code.indexOf("/*", commentStart + 2);
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

        String content = blockComment.substring(2, blockComment.length() - 2).trim();

        String description = content;
        int frontmatterStart = content.startsWith("---") ? content.indexOf('\n') : -1;
        int frontmatterEnd = findFrontmatterEnd(content, frontmatterStart);

        if (frontmatterEnd >= 0) {
            String frontmatter = content.substring(frontmatterStart + 1, frontmatterEnd);
            result.putAll(parseFrontmatter(frontmatter));
            description = content.substring(frontmatterEnd);
        }

        description = description.trim();

        if (!description.isEmpty()) {
            result.put("description", description);
        }

        return result;
    }

    private static int findFrontmatterEnd(String content, int openingLineEnd) {
        if (openingLineEnd < 0 || !isWhitespace(content, 3, openingLineEnd)) {
            return -1;
        }

        int lineStart = openingLineEnd + 1;
        while (lineStart < content.length()) {
            int lineEnd = content.indexOf('\n', lineStart);
            if (lineEnd < 0) {
                return -1;
            }
            if (lineStart > openingLineEnd + 1
                    && content.startsWith("---", lineStart)
                    && isWhitespace(content, lineStart + 3, lineEnd)) {
                return lineEnd + 1;
            }
            lineStart = lineEnd + 1;
        }
        return -1;
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
