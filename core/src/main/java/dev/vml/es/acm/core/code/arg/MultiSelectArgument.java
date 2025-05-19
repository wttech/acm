package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.Argument;
import dev.vml.es.acm.core.code.ArgumentType;
import dev.vml.es.acm.core.util.ObjectUtils;
import dev.vml.es.acm.core.util.StringUtil;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MultiSelectArgument<V> extends Argument<V> {

    private Display display = Display.AUTO;

    private Map<String, V> options = new LinkedHashMap<>();

    public MultiSelectArgument(String name) {
        super(name, ArgumentType.MULTISELECT);
    }

    @Override
    public void setValue(V value) {
        if (!ObjectUtils.isCollectionOrArray(value)) {
            throw new IllegalArgumentException(
                    String.format("Multi-select value must be a collection but specified '%s'!", value));
        }
        super.setValue(value);
    }

    public Map<String, V> getOptions() {
        return options;
    }

    public void setOptions(Map<String, V> options) {
        this.options = options;
    }

    public void setOptions(Collection<V> options) {
        this.options = options.stream()
                .collect(Collectors.toMap(
                        k -> StringUtil.capitalizeWords(ObjectUtils.toString(k)), Function.identity()));
    }

    public Display getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = Display.of(display);
    }

    public void setDisplay(Display render) {
        this.display = render;
    }

    public void list() {
        this.display = Display.LIST;
    }

    public void checkbox() {
        this.display = Display.CHECKBOX;
    }

    public enum Display {
        AUTO,
        LIST,
        CHECKBOX;

        public static Display of(String name) {
            return Arrays.stream(Display.values())
                    .filter(r -> r.name().equalsIgnoreCase(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            String.format("Multi-select argument cannot be displayed as '%s'!", name)));
        }
    }
}
