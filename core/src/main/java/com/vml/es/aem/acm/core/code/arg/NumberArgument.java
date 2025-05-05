package com.vml.es.aem.acm.core.code.arg;

import com.vml.es.aem.acm.core.code.Argument;
import com.vml.es.aem.acm.core.code.ArgumentType;

public class NumberArgument extends Argument<Number> {
    private Number min;

    private Number max;

    private Number step;

    public NumberArgument(String name) {
        super(name, ArgumentType.SLIDER);
    }

    public Number getMin() {
        return min;
    }

    public void setMin(Number min) {
        this.min = min;
    }

    public Number getMax() {
        return max;
    }

    public void setMax(Number max) {
        this.max = max;
    }

    public Number getStep() {
        return step;
    }

    public void setStep(Number step) {
        this.step = step;
    }
}
