package com.wttech.aem.contentor.core.snippet;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;

import java.io.InputStream;
import java.io.Serializable;

public class SnippetDefinition implements Serializable {

    private String name;

    private String content;

    private String documentation;

    public SnippetDefinition() {
        // for deserialization
    }

    public static SnippetDefinition fromYml(InputStream inputStream) {
        Yaml yaml = new Yaml();
        yaml.setBeanAccess(BeanAccess.FIELD);
        return yaml.loadAs(inputStream, SnippetDefinition.class);
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public String getDocumentation() {
        return documentation;
    }
}
