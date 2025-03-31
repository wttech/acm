package com.wttech.aem.acm.core.code.arg;

import com.wttech.aem.acm.core.code.Argument;
import com.wttech.aem.acm.core.code.ArgumentType;
import com.wttech.aem.acm.core.util.ObjectUtils;
import java.util.*;

public class MultiSelectArgument<V> extends Argument<V> {

    private Display display = Display.LIST;

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
