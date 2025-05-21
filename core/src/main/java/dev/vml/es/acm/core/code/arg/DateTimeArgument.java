package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.Argument;
import dev.vml.es.acm.core.code.ArgumentType;
import dev.vml.es.acm.core.util.DateUtils;
import java.time.LocalDateTime;

public class DateTimeArgument extends Argument<LocalDateTime> {

    private LocalDateTime min;

    private LocalDateTime max;

    public DateTimeArgument(String name) {
        super(name, ArgumentType.DATETIME);
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

    public void setMax(LocalDateTime max) {
        this.max = max;
    }

    public void setValue(String value) {
        setValue(DateUtils.toLocalDateTime(value));
    }
}
