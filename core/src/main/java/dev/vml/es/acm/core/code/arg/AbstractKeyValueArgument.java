package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.Argument;
import dev.vml.es.acm.core.code.ArgumentType;

abstract class AbstractKeyValueArgument<V> extends Argument<V> {

    private String keyLabel;

    private String valueLabel;

    public AbstractKeyValueArgument(String name, ArgumentType type, Class<?> valueType) {
        super(name, type, valueType);
    }

    public String getKeyLabel() {
        return keyLabel;
    }

    public void setKeyLabel(String keyLabel) {
        this.keyLabel = keyLabel;
    }

    public String getValueLabel() {
        return valueLabel;
    }

    public void setValueLabel(String valueLabel) {
        this.valueLabel = valueLabel;
    }
}
