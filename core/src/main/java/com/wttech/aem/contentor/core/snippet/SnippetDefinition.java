package com.wttech.aem.contentor.core.snippet;

import com.wttech.aem.contentor.core.util.YamlUtils;
import java.io.InputStream;
import java.io.Serializable;

public class SnippetDefinition implements Serializable {

    private String name;

    private String group;

    private String content;

    private String documentation;

    public SnippetDefinition() {
        // for deserialization
    }

    public static SnippetDefinition fromYaml(String path, InputStream inputStream) {
        try {
            return YamlUtils.readYaml(inputStream, SnippetDefinition.class);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Snippet definition at path '%s' cannot be parsed as YML!", path), e);
        }
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public String getContent() {
        return content;
    }

    public String getDocumentation() {
        return documentation;
    }
}
