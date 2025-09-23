package dev.vml.es.acm.core.code.input;

import dev.vml.es.acm.core.code.Input;
import dev.vml.es.acm.core.code.InputType;
import java.util.Arrays;

public class StringInput extends Input<String> {

    private Display display = Display.TEXT;

    public StringInput(String name) {
        super(name, InputType.STRING, String.class);
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
                            String.format("String input cannot be displayed as '%s'!", name)));
        }
    }
}
