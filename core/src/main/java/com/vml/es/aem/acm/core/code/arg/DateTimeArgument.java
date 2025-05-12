package com.vml.es.aem.acm.core.code.arg;

import com.vml.es.aem.acm.core.code.Argument;
import com.vml.es.aem.acm.core.code.ArgumentType;
import com.vml.es.aem.acm.core.util.DateUtils;
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

    public void setMin(int year, int month, int day) {
        this.min = LocalDateTime.of(year, month, day, 0, 0, 0);
    }

    public void setMin(int year, int month, int day, int hour, int minute, int second) {
        this.min = LocalDateTime.of(year, month, day, hour, minute, second);
    }

    public void setMin(String min) {
        this.min = DateUtils.toLocalDateTime(min);
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

    public void setMax(int year, int month, int day) {
        this.max = LocalDateTime.of(year, month, day, 23, 59, 59);
    }

    public void setMax(int year, int month, int day, int hour, int minute, int second) {
        this.max = LocalDateTime.of(year, month, day, hour, minute, second);
    }

    public void setMax(String max) {
        this.max = DateUtils.toLocalDateTime(max);
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

    public void setValue(LocalDate value) {
        setValue(value.atStartOfDay());
    }

    public void setValue(int year, int month, int day) {
        setValue(LocalDate.of(year, month, day));
    }

    public void setValue(int year, int month, int day, int hour, int minute, int second) {
        setValue(LocalDateTime.of(year, month, day, hour, minute, second));
    }

    public void setValue(String value) {
        setValue(DateUtils.toLocalDateTime(value));
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

    public LocalDateTime now() {
        return LocalDateTime.now();
    }

    public LocalDateTime startOfToday() {
        return LocalDate.now().atStartOfDay();
    }

    public LocalDateTime endOfToday() {
        return LocalDate.now().atTime(23, 59, 59);
    }
}
