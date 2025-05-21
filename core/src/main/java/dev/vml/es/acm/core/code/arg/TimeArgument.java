package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.Argument;
import dev.vml.es.acm.core.code.ArgumentType;
import dev.vml.es.acm.core.util.DateUtils;
import java.time.LocalTime;
import java.util.List;

public class TimeArgument extends Argument<LocalTime> {

    private LocalTime min;

    private LocalTime max;

    public TimeArgument(String name) {
        super(name, ArgumentType.TIME, LocalTime.class);
    }

    public LocalTime getMin() {
        return min;
    }

    public LocalTime getMax() {
        return max;
    }

    public void setMin(String min) {
        setMin(DateUtils.toLocalTime(min));
    }

    public void setMin(LocalTime min) {
        this.min = min;
    }

    public void setMin(int hour, int minute, int second) {
        setMin(LocalTime.of(hour, minute, second));
    }

    public void setMin(List<Integer> values) {
        if (values.size() != 3) {
            throw new IllegalArgumentException(
                    String.format("Time min value must be a list of 3 elements but specified '%s'!", values));
        }
        setMin(values.get(0), values.get(1), values.get(2));
    }

    public void setMax(String max) {
        setMax(DateUtils.toLocalTime(max));
    }

    public void setMax(LocalTime max) {
        this.max = max;
    }

    public void setMax(int hour, int minute, int second) {
        setMax(LocalTime.of(hour, minute, second));
    }

    public void setMax(List<Integer> values) {
        if (values.size() != 3) {
            throw new IllegalArgumentException(
                    String.format("Time max value must be a list of 3 elements but specified '%s'!", values));
        }
        setMax(values.get(0), values.get(1), values.get(2));
    }

    public void setValue(String value) {
        setValue(DateUtils.toLocalTime(value));
    }

    public void setValue(int hour, int minute, int second) {
        setValue(LocalTime.of(hour, minute, second));
    }

    public void setValue(List<Integer> values) {
        if (values.size() != 3) {
            throw new IllegalArgumentException(
                    String.format("Time value must be a list of 3 elements but specified '%s'!", values));
        }
        setValue(values.get(0), values.get(1), values.get(2));
    }

    public LocalTime now() {
        return LocalTime.now();
    }

    public LocalTime startOfDay() {
        return LocalTime.MIN;
    }

    public LocalTime endOfDay() {
        return LocalTime.MAX;
    }
}
