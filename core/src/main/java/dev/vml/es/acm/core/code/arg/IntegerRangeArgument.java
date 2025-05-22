package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.Argument;
import dev.vml.es.acm.core.code.ArgumentType;
import dev.vml.es.acm.core.util.Range;
import java.util.List;

public class IntegerRangeArgument extends Argument<Range<Integer>> {
    private Integer min;

    private Integer max;

    private Integer step;

    public IntegerRangeArgument(String name) {
        super(name, ArgumentType.NUMBER_RANGE, null);
    }

    public void setValue(List<Integer> value) {
        if (value.size() != 2) {
            throw new IllegalArgumentException(
                    String.format("Range value must be a list of two elements but specified '%s'!", value));
        }
        super.setValue(new Range<>(value.get(0), value.get(1)));
    }

    public Integer getMin() {
        return min;
    }

    public void setMin(Integer min) {
        this.min = min;
    }

    public Integer getStep() {
        return step;
    }

    public void setStep(Integer step) {
        this.step = step;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }
}
