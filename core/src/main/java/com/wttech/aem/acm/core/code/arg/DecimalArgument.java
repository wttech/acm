package com.wttech.aem.acm.core.code.arg;

import com.wttech.aem.acm.core.code.Argument;
import com.wttech.aem.acm.core.code.ArgumentType;
import java.math.BigDecimal;

public class DecimalArgument extends Argument<BigDecimal> {

    private BigDecimal min;

    private BigDecimal max;

    public DecimalArgument(String name) {
        super(name, ArgumentType.DECIMAL);
    }

    public BigDecimal getMin() {
        return min;
    }

    public void setMin(BigDecimal min) {
        this.min = min;
    }

    public BigDecimal getMax() {
        return max;
    }

    public void setMax(BigDecimal max) {
        this.max = max;
    }
}
