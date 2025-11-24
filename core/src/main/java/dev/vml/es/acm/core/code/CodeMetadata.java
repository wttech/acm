package dev.vml.es.acm.core.code;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.util.YamlUtils;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodeMetadata implements Serializable {

    public static final CodeMetadata EMPTY = new CodeMetadata(new LinkedHashMap<>());

    private static final Logger LOG = LoggerFactory.getLogger(CodeMetadata.class);

    private static final Pattern BLOCK_COMMENT_PATTERN =
            Pattern.compile("/\\*(?!\\*)([^*]|\\*(?!/))*\\*/", Pattern.DOTALL);
    private static final Pattern FRONTMATTER_PATTERN =
            Pattern.compile("^---\\s*\\n(.+?)^---\\s*\\n", Pattern.DOTALL | Pattern.MULTILINE);
    private static final Pattern NEWLINE_AFTER_COMMENT = Pattern.compile("^\\s*\\n[\\s\\S]*");
    private static final Pattern BLANK_LINE_AFTER_COMMENT = Pattern.compile("^\\s*\\n\\s*\\n[\\s\\S]*");
    private static final Pattern IMPORT_OR_PACKAGE_BEFORE =
            Pattern.compile("[\\s\\S]*(import|package)[\\s\\S]*\\n\\s*\\n\\s*$");
    private static final Pattern COMMENT_MARKERS = Pattern.compile("^/\\*|\\*/$");

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
            String blockComment = findFirstBlockComment(code);
            if (blockComment != null) {
                return new CodeMetadata(parseBlockComment(blockComment));
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
        Matcher matcher = BLOCK_COMMENT_PATTERN.matcher(code);

        while (matcher.find()) {
            String comment = matcher.group();
            int commentStart = matcher.start();
            int commentEnd = matcher.end();

            String afterComment = code.substring(commentEnd);

            if (!NEWLINE_AFTER_COMMENT.matcher(afterComment).matches()) {
                continue;
            }

            if (!BLANK_LINE_AFTER_COMMENT.matcher(afterComment).matches()) {
                continue;
            }

            if (commentStart > 0) {
                String beforeComment = code.substring(0, commentStart);
                String trimmedBefore = beforeComment.trim();
                if (trimmedBefore.isEmpty()
                        || IMPORT_OR_PACKAGE_BEFORE.matcher(beforeComment).matches()) {
                    return comment;
                }
            } else {
                return comment;
            }
        }

        return null;
    }

    /**
     * Extracts frontmatter (YAML between triple dashes) and description from block comment.
     */
    private static Map<String, Object> parseBlockComment(String blockComment) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (StringUtils.isBlank(blockComment)) {
            return result;
        }

        String content = COMMENT_MARKERS.matcher(blockComment).replaceAll("").trim();

        Matcher frontmatterMatcher = FRONTMATTER_PATTERN.matcher(content);
        String description = content;

        if (frontmatterMatcher.find()) {
            String frontmatter = frontmatterMatcher.group(1);
            if (frontmatter != null) {
                result.putAll(parseFrontmatter(frontmatter));
            }
            description = content.substring(frontmatterMatcher.end());
        }

        description = description.trim();

        if (!description.isEmpty()) {
            result.put("description", description);
        }

        return result;
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
