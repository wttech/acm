package com.vml.es.aem.acm.core.code.arg;

import com.vml.es.aem.acm.core.code.Argument;
import com.vml.es.aem.acm.core.code.ArgumentType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

public class DateTimeArgument extends Argument<LocalDateTime> {
    private DateTimeArgument.Display display = DateTimeArgument.Display.DATETIME;

    public DateTimeArgument(String name) {
        super(name, ArgumentType.DATETIME);
    }

    public DateTimeArgument.Display getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = DateTimeArgument.Display.of(display);
    }

    public void setDisplay(DateTimeArgument.Display render) {
        this.display = render;
    }

    // Cast LocalDate to LocalDateTime in case of DATE display type
    public void setValue(LocalDate value) {
        setValue(value.atStartOfDay());
    }

    public void date() {
        this.display = DateTimeArgument.Display.DATE;
    }

    public void dateTime() {
        this.display = DateTimeArgument.Display.DATETIME;
    }

    public enum Display {
        DATE,
        DATETIME;

        public static DateTimeArgument.Display of(String name) {
            return Arrays.stream(DateTimeArgument.Display.values())
                    .filter(r -> r.name().equalsIgnoreCase(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            String.format("Bool argument cannot be displayed as '%s'!", name)));
        }
    }
}
