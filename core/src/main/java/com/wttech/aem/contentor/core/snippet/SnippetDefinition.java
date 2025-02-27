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

    public static SnippetDefinition fromYaml(InputStream inputStream) {
        return YamlUtils.readYaml(inputStream, SnippetDefinition.class);
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
