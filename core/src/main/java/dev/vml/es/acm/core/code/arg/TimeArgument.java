package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.Argument;
import dev.vml.es.acm.core.code.ArgumentType;
import dev.vml.es.acm.core.util.DateUtils;
import java.time.LocalTime;

public class TimeArgument extends Argument<LocalTime> {

    private LocalTime min;

    private LocalTime max;

    public TimeArgument(String name) {
        super(name, ArgumentType.TIME);
    }

    public LocalTime getMin() {
        return min;
    }

    public LocalTime getMax() {
        return max;
    }

    public void setMin(LocalTime min) {
        this.min = min;
    }

    public void setMax(LocalTime max) {
        this.max = max;
    }

    public void setValue(String value) {
        setValue(DateUtils.toLocalTime(value));
    }
}
