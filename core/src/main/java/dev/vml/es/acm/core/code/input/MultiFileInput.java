package dev.vml.es.acm.core.code.input;

import dev.vml.es.acm.core.code.InputType;

public class MultiFileInput extends AbstractFileInput<String[]> {

    private Integer min;

    private Integer max;

    public MultiFileInput(String name) {
        super(name, InputType.MULTIFILE, String[].class);
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
}
