package dev.vml.es.acm.core.code;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.vml.es.acm.core.code.input.*;
import dev.vml.es.acm.core.util.GroovyUtils;
import dev.vml.es.acm.core.util.TypeUtils;
import dev.vml.es.acm.core.util.TypeValueMap;
import groovy.lang.Closure;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.sling.api.resource.ValueMap;

public class Inputs implements Serializable {

    private final Map<String, Input<?>> definitions = new LinkedHashMap<>();

    public Inputs() {
        super();
    }

    public Input<?> get(String name) {
        Input<?> result = definitions.get(name);
        if (result == null) {
            throw new IllegalArgumentException(String.format("Input '%s' is not defined!", name));
        }
        return result;
    }

    private void add(Input<?> input) {
        if (definitions.containsKey(input.getName())) {
            throw new IllegalArgumentException(
                    String.format("Input with name '%s' is already defined!", input.getName()));
        }
        definitions.put(input.getName(), input);
    }

    @JsonAnyGetter
    public Map<String, Input<?>> getDefinitions() {
        return definitions;
    }

    @JsonIgnore
    public ValueMap getValues() {
        Map<String, Object> props = new HashMap<>();
        for (Input<?> input : definitions.values()) {
            props.put(input.getName(), input.getValue());
        }
        return new TypeValueMap(props);
    }

    @JsonIgnore
    public ValueMap values() {
        return getValues();
    }

    public <T> T getValue(String name, Class<T> type) {
        return getValues().get(name, type);
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(String name) {
        return (T) getValues().get(name);
    }

    public <T> T value(String name, Class<T> type) {
        return getValue(name, type);
    }

    public <T> T value(String name) {
        return getValue(name);
    }

    public void setValues(InputValues inputs) {
        if (inputs == null) {
            return; // may be skipped then default values will be used
        }
        inputs.forEach((name, value) -> {
            Input<?> result = get(name);
            setValue(result, value);
        });
    }

    @SuppressWarnings("unchecked")
    private <T> void setValue(Input<T> input, Object value) {
        Class<?> valueType = input.getValueType();
        if (valueType == null) {
            input.setValue((T) value);
        } else {
            Optional<?> convertedValue = TypeUtils.convert(value, valueType, true);
            if (convertedValue.isPresent()) {
                input.setValue((T) convertedValue.get());
            } else {
                input.setValue((T) value);
            }
        }
    }

    public void bool(String name) {
        bool(name, null);
    }

    public void bool(String name, Closure<BoolInput> options) {
        BoolInput result = new BoolInput(name);
        GroovyUtils.with(result, options);
        add(result);
    }

    public void dateTime(String name) {
        dateTime(name, null);
    }

    public void dateTime(String name, Closure<DateTimeInput> options) {
        DateTimeInput result = new DateTimeInput(name);
        GroovyUtils.with(result, options);
        add(result);
    }

    public void date(String name) {
        date(name, null);
    }

    public void date(String name, Closure<DateInput> options) {
        DateInput result = new DateInput(name);
        GroovyUtils.with(result, options);
        add(result);
    }

    public void time(String name) {
        time(name, null);
    }

    public void time(String name, Closure<TimeInput> options) {
        TimeInput result = new TimeInput(name);
        GroovyUtils.with(result, options);
        add(result);
    }

    public void string(String name) {
        string(name, null);
    }

    public void string(String name, Closure<StringInput> options) {
        StringInput result = new StringInput(name);
        GroovyUtils.with(result, options);
        add(result);
    }

    public void text(String name) {
        text(name, null);
    }

    public void text(String name, Closure<TextInput> options) {
        TextInput result = new TextInput(name);
        GroovyUtils.with(result, options);
        add(result);
    }

    public void select(String name) {
        select(name, null);
    }

    public <V> void select(String name, Closure<SelectInput<V>> options) {
        SelectInput<V> result = new SelectInput<>(name);
        GroovyUtils.with(result, options);
        add(result);
    }

    public void multiSelect(String name) {
        multiSelect(name, null);
    }

    public <V> void multiSelect(String name, Closure<MultiSelectInput<V>> options) {
        MultiSelectInput<V> result = new MultiSelectInput<>(name);
        GroovyUtils.with(result, options);
        add(result);
    }

    public void integerNumber(String name) {
        integerNumber(name, null);
    }

    public void integerNumber(String name, Closure<IntegerInput> options) {
        IntegerInput result = new IntegerInput(name);
        GroovyUtils.with(result, options);
        add(result);
    }

    public void color(String name) {
        color(name, null);
    }

    public void color(String name, Closure<ColorInput> options) {
        ColorInput result = new ColorInput(name);
        GroovyUtils.with(result, options);
        add(result);
    }

    public void integerRange(String name, Closure<IntegerRangeInput> options) {
        IntegerRangeInput result = new IntegerRangeInput(name);
        GroovyUtils.with(result, options);
        add(result);
    }

    public void decimalRange(String name) {
        decimalRange(name, null);
    }

    public void decimalRange(String name, Closure<DecimalRangeInput> options) {
        DecimalRangeInput result = new DecimalRangeInput(name);
        GroovyUtils.with(result, options);
        add(result);
    }

    public void decimalNumber(String name) {
        decimalNumber(name, null);
    }

    public void decimalNumber(String name, Closure<DecimalInput> options) {
        DecimalInput result = new DecimalInput(name);
        GroovyUtils.with(result, options);
        add(result);
    }

    public void path(String name) {
        text(name, null);
    }

    public void path(String name, Closure<PathInput> options) {
        PathInput result = new PathInput(name);
        GroovyUtils.with(result, options);
        add(result);
    }

    public void file(String name) {
        file(name, null);
    }

    public void file(String name, Closure<FileInput> options) {
        FileInput result = new FileInput(name);
        GroovyUtils.with(result, options);
        add(result);
    }

    public void multiFile(String name) {
        multiFile(name, null);
    }

    public void multiFile(String name, Closure<MultiFileInput> options) {
        MultiFileInput result = new MultiFileInput(name);
        GroovyUtils.with(result, options);
        add(result);
    }

    public void map(String name) {
        map(name, null);
    }

    public void map(String name, Closure<MapInput> options) {
        MapInput result = new MapInput(name);
        GroovyUtils.with(result, options);
        add(result);
    }

    public void keyValueList(String name) {
        keyValueList(name, null);
    }

    public void keyValueList(String name, Closure<KeyValueListInput> options) {
        KeyValueListInput result = new KeyValueListInput(name);
        GroovyUtils.with(result, options);
        add(result);
    }
}
