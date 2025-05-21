package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.Argument;
import dev.vml.es.acm.core.code.ArgumentType;
import dev.vml.es.acm.core.util.DateUtils;
import java.time.LocalDate;
import java.util.List;

public class DateArgument extends Argument<LocalDate> {

    private LocalDate min;

    private LocalDate max;

    public DateArgument(String name) {
        super(name, ArgumentType.DATE);
    }

    public LocalDate getMin() {
        return min;
    }

    public LocalDate getMax() {
        return max;
    }

    public void setMin(LocalDate min) {
        this.min = min;
    }

    public void setMin(String min) {
        setMin(DateUtils.toLocalDate(min));
    }

    public void setMin(int year, int month, int dayOfMonth) {
        setMin(LocalDate.of(year, month, dayOfMonth));
    }

    public void setMin(List<Integer> values) {
        if (values.size() != 3) {
            throw new IllegalArgumentException(
                    String.format("Date min value must be a list of 3 elements but specified '%s'!", values));
        }
        setMin(values.get(0), values.get(1), values.get(2));
    }

    public void setMax(LocalDate max) {
        this.max = max;
    }

    public void setMax(String max) {
        setMax(DateUtils.toLocalDate(max));
    }

    public void setMax(int year, int month, int dayOfMonth) {
        setMax(LocalDate.of(year, month, dayOfMonth));
    }

    public void setMax(List<Integer> values) {
        if (values.size() != 3) {
            throw new IllegalArgumentException(
                    String.format("Date max value must be a list of 3 elements but specified '%s'!", values));
        }
        setMax(values.get(0), values.get(1), values.get(2));
    }

    public void setValue(String value) {
        setValue(DateUtils.toLocalDate(value));
    }

    public void setValue(int year, int month, int dayOfMonth) {
        setValue(LocalDate.of(year, month, dayOfMonth));
    }

    public void setValue(List<Integer> values) {
        if (values.size() != 3) {
            throw new IllegalArgumentException(
                    String.format("Date value must be a list of 3 elements but specified '%s'!", values));
        }
        setValue(values.get(0), values.get(1), values.get(2));
    }

    public LocalDate now() {
        return LocalDate.now();
    }
}
