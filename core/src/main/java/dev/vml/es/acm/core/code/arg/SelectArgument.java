package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.Argument;
import dev.vml.es.acm.core.code.ArgumentType;
import dev.vml.es.acm.core.util.ObjectUtils;
import dev.vml.es.acm.core.util.StringUtil;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SelectArgument<V> extends Argument<V> {

    private Display display = Display.AUTO;

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

    public void dropdown() {
        this.display = Display.DROPDOWN;
    }

    public void radio() {
        this.display = Display.RADIO;
    }

    public enum Display {
        AUTO,
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
