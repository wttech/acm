package dev.vml.es.acm.core.code.input;

import dev.vml.es.acm.core.code.Input;
import dev.vml.es.acm.core.code.InputType;
import dev.vml.es.acm.core.util.Range;
import java.util.List;

public class IntegerRangeInput extends Input<Range<Integer>> {

    private Integer min;

    private Integer max;

    private Integer step;

    public IntegerRangeInput(String name) {
        super(name, InputType.NUMBER_RANGE, null);
    }

    public void setValue(List<Integer> value) {
        if (value == null) {
            super.setValue(null);
        } else {
            if (value.size() != 2) {
                throw new IllegalArgumentException(
                        String.format("Range value must be a list of two elements but specified '%s'!", value));
            }
            super.setValue(new Range<>(value.get(0), value.get(1)));
        }
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
