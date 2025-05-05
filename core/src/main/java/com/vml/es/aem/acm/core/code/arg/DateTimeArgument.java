package com.vml.es.aem.acm.core.code.arg;

import com.vml.es.aem.acm.core.code.Argument;
import com.vml.es.aem.acm.core.code.ArgumentType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

public class DateTimeArgument extends Argument<LocalDateTime> {

    private DateTimeArgument.Variant variant = DateTimeArgument.Variant.DATETIME;

    private LocalDateTime min;

    private LocalDateTime max;

    public DateTimeArgument(String name) {
        super(name, ArgumentType.DATETIME);
    }

    public LocalDateTime getMin() {
        return min;
    }

    public void setMin(LocalDateTime min) {
        this.min = min;
    }

    public void setMin(LocalDate min) {
        this.min = min.atStartOfDay();
    }

    public LocalDateTime getMax() {
        return max;
    }

    public void setMax(LocalDateTime max) {
        this.max = max;
    }

    public void setMax(LocalDate max) {
        this.max = max.atStartOfDay();
    }

    public DateTimeArgument.Variant getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = DateTimeArgument.Variant.of(variant);
    }

    public void setVariant(DateTimeArgument.Variant render) {
        this.variant = render;
    }

    // Cast LocalDate to LocalDateTime in case of DATE variant
    public void setValue(LocalDate value) {
        setValue(value.atStartOfDay());
    }

    public void date() {
        this.variant = DateTimeArgument.Variant.DATE;
    }

    public void dateTime() {
        this.variant = DateTimeArgument.Variant.DATETIME;
    }

    public enum Variant {
        DATE,
        DATETIME;

        public static DateTimeArgument.Variant of(String name) {
            return Arrays.stream(DateTimeArgument.Variant.values())
                    .filter(r -> r.name().equalsIgnoreCase(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            String.format("Datetime variant '%s' is not supported!", name)));
        }
    }
}
