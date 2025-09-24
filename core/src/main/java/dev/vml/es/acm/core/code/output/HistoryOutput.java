package dev.vml.es.acm.core.code.output;

import dev.vml.es.acm.core.code.Output;
import java.util.HashMap;
import java.util.Map;

public class HistoryOutput extends Output {

    public HistoryOutput(String name) {
        super(name);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> props = new HashMap<>();
        props.put("label", getLabel());
        props.put("description", getDescription());
        props.put("downloadName", getDownloadName());
        return props;
    }
}
