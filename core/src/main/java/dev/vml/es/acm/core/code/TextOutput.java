package dev.vml.es.acm.core.code;

import java.util.Map;
import java.util.stream.Collectors;

public class TextOutput extends Output {

    private String text;

    public TextOutput() {
        super(); // for deserialization
    }

    public TextOutput(String name) {
        super(name, OutputType.TEXT);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setLinks(Map<String, String> links) {
        text = links.entrySet().stream()
            .map(entry -> "- [" + entry.getKey() + "](" + entry.getValue() + ")")
            .collect(Collectors.joining(System.lineSeparator()));
    }
}
