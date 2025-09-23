package dev.vml.es.acm.core.code.input;

import dev.vml.es.acm.core.code.InputType;
import java.io.File;

public class MultiFileInput extends AbstractFileInput<File[]> {

    private Integer min;

    private Integer max;

    public MultiFileInput(String name) {
        super(name, InputType.MULTIFILE, File[].class);
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
