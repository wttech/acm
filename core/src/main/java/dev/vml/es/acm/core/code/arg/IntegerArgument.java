package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.Argument;
import dev.vml.es.acm.core.code.ArgumentType;
import java.util.Arrays;

public class IntegerArgument extends Argument<Integer> {

    private Integer min;

    private Integer max;

    private Display display = Display.INPUT;

    private Integer step;

    public IntegerArgument(String name) {
        super(name, ArgumentType.INTEGER);
    }

    public Integer getMin() {
        return min;
    }

    public void setMin(Integer min) {
        this.min = min;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    public void input() {
        this.display = Display.INPUT;
    }

    public void slider() {
        this.display = Display.SLIDER;
    }

    public void slider(Integer min, Integer max) {
        this.display = Display.SLIDER;
        this.min = min;
        this.max = max;
    }

    public Display getDisplay() {
        return display;
    }

    public Integer getStep() {
        return step;
    }

    public void setStep(Integer step) {
        this.step = step;
    }

    public enum Display {
        INPUT,
        SLIDER;

        public static IntegerArgument.Display of(String name) {
            return Arrays.stream(IntegerArgument.Display.values())
                    .filter(r -> r.name().equalsIgnoreCase(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            String.format("Decimal argument cannot be displayed as '%s'!", name)));
        }
    }
}
