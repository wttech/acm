package dev.vml.es.acm.core.format;

public class Formatter {

    private final TemplateFormatter templateFormatter = new TemplateFormatter();

    private final JsonFormatter json = new JsonFormatter();

    private final YamlFormatter yaml = new YamlFormatter();

    private final Base64Formatter base64 = new Base64Formatter();

    public TemplateFormatter getTemplate() {
        return templateFormatter;
    }

    public JsonFormatter getJson() {
        return json;
    }

    public YamlFormatter getYaml() {
        return yaml;
    }

    public Base64Formatter getBase64() {
        return base64;
    }
}
