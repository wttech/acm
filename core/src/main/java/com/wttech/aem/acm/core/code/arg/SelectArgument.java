package com.wttech.aem.acm.core.code.arg;

import com.wttech.aem.acm.core.code.Argument;
import com.wttech.aem.acm.core.code.ArgumentType;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class SelectArgument<V> extends Argument<V> {

    private Display display = Display.DROPDOWN;

    private Map<String, V> options = new LinkedHashMap<>();

    public SelectArgument(String name) {
        super(name, ArgumentType.SELECT);
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

    public void dropdown() {
        this.display = Display.DROPDOWN;
    }

    public void radio() {
        this.display = Display.RADIO;
    }

    public enum Display {
        DROPDOWN,
        RADIO;

        public static Display of(String name) {
            return Arrays.stream(Display.values())
                    .filter(r -> r.name().equalsIgnoreCase(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            String.format("Select argument cannot be displayed as '%s'!", name)));
        }
    }
}
