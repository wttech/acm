package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.Argument;
import dev.vml.es.acm.core.code.ArgumentType;
import java.util.Arrays;

public class BoolArgument extends Argument<Boolean> {

    private Display display = Display.SWITCHER;

    public BoolArgument(String name) {
        super(name, ArgumentType.BOOL, Boolean.class);
        setRequired(false);
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

    public void switcher() {
        this.display = Display.SWITCHER;
    }

    public void checkbox() {
        this.display = Display.CHECKBOX;
    }

    public enum Display {
        SWITCHER,
        CHECKBOX;

        public static Display of(String name) {
            return Arrays.stream(Display.values())
                    .filter(r -> r.name().equalsIgnoreCase(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            String.format("Bool argument cannot be displayed as '%s'!", name)));
        }
    }
}
