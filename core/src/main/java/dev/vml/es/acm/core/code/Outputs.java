package dev.vml.es.acm.core.code;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import dev.vml.es.acm.core.util.GroovyUtils;
import groovy.lang.Closure;
import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class Outputs implements Serializable, Closeable {

    private final Map<String, Output> definitions = new LinkedHashMap<>();

    private final transient ExecutionContext executionContext;

    public Outputs(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

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

    @Deprecated
    public FileOutput make(String name) {
        return file(name);
    }

    @Deprecated
    public FileOutput make(String name, Closure<FileOutput> options) {
        return file(name, options);
    }

    public FileOutput file(String name) {
        return file(name, null);
    }

    public FileOutput file(String name, Closure<FileOutput> options) {
        FileOutput result = new FileOutput(name, executionContext);
        GroovyUtils.with(result, options);
        add(result);
        return result;
    }

    @Override
    public void close() {
        for (Output output : getDefinitions().values()) {
            if (output instanceof Closeable) {
                try {
                    ((Closeable) output).close();
                } catch (IOException e) {
                    throw new UncheckedIOException(String.format("Output '%s' cannot be closed!", output.getName()), e);
                }
            }
        }
    }
}
