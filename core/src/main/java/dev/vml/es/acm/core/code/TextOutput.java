package dev.vml.es.acm.core.code;

import java.util.Map;
import java.util.stream.Collectors;

public class TextOutput extends Output {

    private String value;

    public TextOutput() {
        super(); // for deserialization
    }

    public TextOutput(String name) {
        super(name);
    }

    @Override
    public OutputType getType() {
        return OutputType.TEXT;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String text) {
        this.value = text;
    }

    // Utility methods

    public String link(String name, String url) {
        return "[" + name + "](" + url + ")";
    }

    public String links(Map<String, String> links) {
        return links.entrySet().stream()
                .map(entry -> "- [" + entry.getKey() + "](" + entry.getValue() + ")")
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
