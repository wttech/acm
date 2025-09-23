package dev.vml.es.acm.core.code.input;

import dev.vml.es.acm.core.code.Input;
import dev.vml.es.acm.core.code.InputType;
import dev.vml.es.acm.core.util.Range;
import java.math.BigDecimal;
import java.util.List;

public class DecimalRangeInput extends Input<Range<BigDecimal>> {
    private BigDecimal min;

    private BigDecimal max;

    private BigDecimal step;

    public DecimalRangeInput(String name) {
        super(name, InputType.NUMBER_RANGE, null);
    }

    public void setValue(List<BigDecimal> value) {
        if (value.size() != 2) {
            throw new IllegalArgumentException(
                    String.format("Range value must be a list of two elements but specified '%s'!", value));
        }
        super.setValue(new Range<>(value.get(0), value.get(1)));
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
}
