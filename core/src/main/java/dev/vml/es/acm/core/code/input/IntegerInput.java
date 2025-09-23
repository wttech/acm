package dev.vml.es.acm.core.code.input;

import dev.vml.es.acm.core.code.Input;
import dev.vml.es.acm.core.code.InputType;
import java.util.Arrays;

public class IntegerInput extends Input<Integer> {

    private Integer min;

    private Integer max;

    private Display display = Display.INPUT;

    private Integer step;

    public IntegerInput(String name) {
        super(name, InputType.INTEGER, Integer.class);
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

        public static IntegerInput.Display of(String name) {
            return Arrays.stream(IntegerInput.Display.values())
                    .filter(r -> r.name().equalsIgnoreCase(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            String.format("Decimal input cannot be displayed as '%s'!", name)));
        }
    }
}
