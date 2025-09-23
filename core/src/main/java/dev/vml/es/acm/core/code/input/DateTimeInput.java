package dev.vml.es.acm.core.code.input;

import dev.vml.es.acm.core.code.Input;
import dev.vml.es.acm.core.code.InputType;
import dev.vml.es.acm.core.util.DateUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DateTimeInput extends Input<LocalDateTime> {

    private LocalDateTime min;

    private LocalDateTime max;

    public DateTimeInput(String name) {
        super(name, InputType.DATETIME, LocalDateTime.class);
    }

    public LocalDateTime getMin() {
        return min;
    }

    public LocalDateTime getMax() {
        return max;
    }

    public void setMin(LocalDateTime min) {
        this.min = min;
    }

    public void setMin(String min) {
        setMin(DateUtils.toLocalDateTime(min));
    }

    public void setMin(int year, int month, int dayOfMonth, int hour, int minute, int second) {
        setMin(LocalDateTime.of(year, month, dayOfMonth, hour, minute, second));
    }

    public void setMin(List<Integer> values) {
        if (values.size() != 6) {
            throw new IllegalArgumentException(
                    String.format("Datetime min value must be a list of 6 elements but specified '%s'!", values));
        }
        setMin(values.get(0), values.get(1), values.get(2), values.get(3), values.get(4), values.get(5));
    }

    public void setMax(LocalDateTime max) {
        this.max = max;
    }

    public void setMax(String max) {
        setMax(DateUtils.toLocalDateTime(max));
    }

    public void setMax(int year, int month, int dayOfMonth, int hour, int minute, int second) {
        setMax(LocalDateTime.of(year, month, dayOfMonth, hour, minute, second));
    }

    public void setMax(List<Integer> values) {
        if (values.size() != 6) {
            throw new IllegalArgumentException(
                    String.format("Datetime max value must be a list of 6 elements but specified '%s'!", values));
        }
        setMax(values.get(0), values.get(1), values.get(2), values.get(3), values.get(4), values.get(5));
    }

    public void setValue(String value) {
        setValue(DateUtils.toLocalDateTime(value));
    }

    public void setValue(int year, int month, int dayOfMonth, int hour, int minute, int second) {
        setValue(LocalDateTime.of(year, month, dayOfMonth, hour, minute, second));
    }

    public void setValue(List<Integer> values) {
        if (values.size() != 6) {
            throw new IllegalArgumentException(
                    String.format("Datetime value must be a list of 6 elements but specified '%s'!", values));
        }
        setValue(values.get(0), values.get(1), values.get(2), values.get(3), values.get(4), values.get(5));
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
