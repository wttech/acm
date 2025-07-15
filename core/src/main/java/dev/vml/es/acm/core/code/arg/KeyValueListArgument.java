package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.Argument;
import dev.vml.es.acm.core.code.ArgumentType;
import dev.vml.es.acm.core.util.KeyValue;
import java.util.List;
import java.util.Map;

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
}
