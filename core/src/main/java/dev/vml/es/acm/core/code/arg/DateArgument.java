package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.Argument;
import dev.vml.es.acm.core.code.ArgumentType;
import dev.vml.es.acm.core.util.DateUtils;
import java.time.LocalDate;

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

    public void setMax(LocalDate max) {
        this.max = max;
    }

    public void setValue(String value) {
        setValue(DateUtils.toLocalDate(value));
    }
}
