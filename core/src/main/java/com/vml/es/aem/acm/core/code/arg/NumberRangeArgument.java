package com.vml.es.aem.acm.core.code.arg;

import com.vml.es.aem.acm.core.code.Argument;
import com.vml.es.aem.acm.core.code.ArgumentType;
import java.io.Serializable;
import java.util.List;

public class NumberRangeArgument extends Argument<NumberRangeArgument.Range> {
    private Number min;

    private Number max;

    private Number step;

    public NumberRangeArgument(String name) {
        super(name, ArgumentType.RANGE);
    }

    public void setValue(List<? extends Number> value) {
        if (value.size() != 2) {
            throw new IllegalArgumentException(
                    String.format("Range value must be a list of two elements but specified '%s'!", value));
        }
        super.setValue(new Range(value.get(0), value.get(1)));
    }

    public Number getMin() {
        return min;
    }

    public void setMin(Number min) {
        this.min = min;
    }

    public Number getStep() {
        return step;
    }

    public void setStep(Number step) {
        this.step = step;
    }

    public Number getMax() {
        return max;
    }

    public void setMax(Number max) {
        this.max = max;
    }

    public static class Range implements Serializable {
        private Number start;

        private Number end;

        public Range(Number start, Number end) {
            this.start = start;
            this.end = end;
        }

        public Number getStart() {
            return start;
        }

        public void setStart(Number start) {
            this.start = start;
        }

        public Number getEnd() {
            return end;
        }

        public void setEnd(Number end) {
            this.end = end;
        }
    }
}
