package com.vml.es.aem.acm.core.format;

public class Formatter {

    private final JsonFormatter json = new JsonFormatter();

    private final YamlFormatter yaml = new YamlFormatter();

    public JsonFormatter getJson() {
        return json;
    }

    public YamlFormatter getYaml() {
        return yaml;
    }
}
