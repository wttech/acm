package com.vml.es.aem.acm.core.snippet;

import com.vml.es.aem.acm.core.util.YamlUtils;
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
            return YamlUtils.read(inputStream, SnippetDefinition.class);
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
