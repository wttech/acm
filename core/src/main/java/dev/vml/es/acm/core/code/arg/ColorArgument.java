package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.Argument;
import dev.vml.es.acm.core.code.ArgumentType;
import java.util.Arrays;

public class ColorArgument extends Argument<String> {

    private ColorArgument.Format format = ColorArgument.Format.RGBA;

    public ColorArgument(String name) {
        super(name, ArgumentType.COLOR, String.class);
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = ColorArgument.Format.of(format);
    }

    public void setFormat(ColorArgument.Format format) {
        this.format = format;
    }

    public void hex() {
        this.format = ColorArgument.Format.HEX;
    }

    public void hsl() {
        this.format = ColorArgument.Format.HSL;
    }

    public void rgba() {
        this.format = ColorArgument.Format.RGBA;
    }

    public void hsb() {
        this.format = ColorArgument.Format.HSB;
    }

    public enum Format {
        HEX,
        HSL,
        RGBA,
        HSB;

        public static ColorArgument.Format of(String name) {
            return Arrays.stream(ColorArgument.Format.values())
                    .filter(r -> r.name().equalsIgnoreCase(name))
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalArgumentException(String.format("Color format '%s' is not supported!", name)));
        }
    }
}
