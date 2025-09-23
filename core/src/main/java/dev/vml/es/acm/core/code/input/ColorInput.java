package dev.vml.es.acm.core.code.input;

import dev.vml.es.acm.core.code.Input;
import dev.vml.es.acm.core.code.InputType;
import java.util.Arrays;

public class ColorInput extends Input<String> {

    private ColorInput.Format format = ColorInput.Format.RGBA;

    public ColorInput(String name) {
        super(name, InputType.COLOR, String.class);
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = ColorInput.Format.of(format);
    }

    public void setFormat(ColorInput.Format format) {
        this.format = format;
    }

    public void hex() {
        this.format = ColorInput.Format.HEX;
    }

    public void hsl() {
        this.format = ColorInput.Format.HSL;
    }

    public void rgba() {
        this.format = ColorInput.Format.RGBA;
    }

    public void hsb() {
        this.format = ColorInput.Format.HSB;
    }

    public enum Format {
        HEX,
        HSL,
        RGBA,
        HSB;

        public static ColorInput.Format of(String name) {
            return Arrays.stream(ColorInput.Format.values())
                    .filter(r -> r.name().equalsIgnoreCase(name))
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalArgumentException(String.format("Color format '%s' is not supported!", name)));
        }
    }
}
