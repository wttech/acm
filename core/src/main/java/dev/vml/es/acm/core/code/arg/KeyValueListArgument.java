package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.Argument;
import dev.vml.es.acm.core.code.ArgumentType;
import dev.vml.es.acm.core.util.KeyValue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KeyValueListArgument extends Argument<List<KeyValue<String, String>>> {

    private Integer min;

    private Integer max;

    public KeyValueListArgument(String name) {
        super(name, ArgumentType.MAP, Map.class);
    }

    public Integer getMin() {
        return min;
    }

    public void setMin(Integer min) {
        this.min = min;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    public void setValue(List<List<String>> value) {
        if (value == null) {
            super.setValue(null);
        } else {
            if (value.stream().anyMatch(v -> v.size() != 2)) {
                throw new IllegalArgumentException("Key-value list must contain pairs of key and value!");
            }
            super.setValue(value.stream().map(v -> new KeyValue<>(v.get(0), v.get(1))).collect(Collectors.toList()));
        }
    }
}
