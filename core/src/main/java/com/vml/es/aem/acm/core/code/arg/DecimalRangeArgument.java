package com.vml.es.aem.acm.core.code.arg;

import com.vml.es.aem.acm.core.code.Argument;
import com.vml.es.aem.acm.core.code.ArgumentType;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public class DecimalRangeArgument extends Argument<DecimalRangeArgument.Range> {
    private BigDecimal min;

    private BigDecimal max;

    private BigDecimal step;

    public DecimalRangeArgument(String name) {
        super(name, ArgumentType.RANGE);
    }

    public void setValue(List<BigDecimal> value) {
        if (value.size() != 2) {
            throw new IllegalArgumentException(
                    String.format("Range value must be a list of two elements but specified '%s'!", value));
        }
        super.setValue(new Range(value.get(0), value.get(1)));
    }

    public BigDecimal getMin() {
        return min;
    }

    public void setMin(BigDecimal min) {
        this.min = min;
    }

    public BigDecimal getStep() {
        return step;
    }

    public void setStep(BigDecimal step) {
        this.step = step;
    }

    public BigDecimal getMax() {
        return max;
    }

    public void setMax(BigDecimal max) {
        this.max = max;
    }

    public static class Range implements Serializable {
        private BigDecimal start;

        private BigDecimal end;

        public Range(BigDecimal start, BigDecimal end) {
            this.start = start;
            this.end = end;
        }

        public BigDecimal getStart() {
            return start;
        }

        public void setStart(BigDecimal start) {
            this.start = start;
        }

        public BigDecimal getEnd() {
            return end;
        }

        public void setEnd(BigDecimal end) {
            this.end = end;
        }
    }
}
