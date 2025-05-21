package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.Argument;
import dev.vml.es.acm.core.code.ArgumentType;
import java.util.Arrays;

public class StringArgument extends Argument<String> {

    private Display display = Display.TEXT;

    public StringArgument(String name) {
        super(name, ArgumentType.STRING, String.class);
    }

    public Display getDisplay() {
        return display;
    }

    public void setDisplay(Display display) {
        this.display = display;
    }

    public void password() {
        this.display = Display.PASSWORD;
    }

    public void email() {
        this.display = Display.EMAIL;
    }

    public void url() {
        this.display = Display.URL;
    }

    public void tel() {
        this.display = Display.TEL;
    }

    public enum Display {
        TEXT,
        PASSWORD,
        URL,
        TEL,
        EMAIL;

        public static Display of(String name) {
            return Arrays.stream(Display.values())
                    .filter(r -> r.name().equalsIgnoreCase(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            String.format("String argument cannot be displayed as '%s'!", name)));
        }
    }
}
