package dev.vml.es.acm.core.code;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import dev.vml.es.acm.core.code.output.HistoryOutput;
import dev.vml.es.acm.core.util.GroovyUtils;
import groovy.lang.Closure;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class Outputs implements Serializable {

    private final Map<String, Output> definitions = new LinkedHashMap<>();

    public Output get(String name) {
        Output result = definitions.get(name);
        if (result == null) {
            throw new IllegalArgumentException(String.format("Output '%s' is not defined!", name));
        }
        return result;
    }

    private void add(Output output) {
        if (definitions.containsKey(output.getName())) {
            throw new IllegalArgumentException(
                    String.format("Output with name '%s' is already defined!", output.getName()));
        }
        definitions.put(output.getName(), output);
    }

    @JsonAnyGetter
    public Map<String, Output> getDefinitions() {
        return definitions;
    }

    public HistoryOutput history(String name) {
        return history(name, null);
    }

    public HistoryOutput history(String name, Closure<HistoryOutput> options) {
        HistoryOutput result = new HistoryOutput(name);
        GroovyUtils.with(result, options);
        add(result);
        return result;
    }
}
