package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.Argument;
import dev.vml.es.acm.core.code.ArgumentType;
import java.math.BigDecimal;
import java.util.Arrays;

public class DecimalArgument extends Argument<BigDecimal> {

    private BigDecimal min;

    private BigDecimal max;

    private Display display = Display.INPUT;

    private BigDecimal step;

    public DecimalArgument(String name) {
        super(name, ArgumentType.DECIMAL, BigDecimal.class);
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

    public void input() {
        this.display = Display.INPUT;
    }

    public void slider() {
        this.display = Display.SLIDER;
    }

    public void slider(BigDecimal min, BigDecimal max) {
        this.display = Display.SLIDER;
        this.min = min;
        this.max = max;
    }

    public Display getDisplay() {
        return display;
    }

    public BigDecimal getStep() {
        return step;
    }

    public void setStep(BigDecimal step) {
        this.step = step;
    }

    public enum Display {
        INPUT,
        SLIDER;

        public static DecimalArgument.Display of(String name) {
            return Arrays.stream(DecimalArgument.Display.values())
                    .filter(r -> r.name().equalsIgnoreCase(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            String.format("Decimal argument cannot be displayed as '%s'!", name)));
        }
    }
}
