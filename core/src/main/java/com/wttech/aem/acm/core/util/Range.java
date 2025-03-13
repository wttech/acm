package com.wttech.aem.acm.core.util;

import org.apache.commons.lang3.StringUtils;

public class Range<T extends Comparable<T>> {

    private final T start;

    private final T end;

    public Range(T start, T end) {
        this.start = start;
        this.end = end;
    }

    public static Range<Integer> integersParse(String text) {
        if (StringUtils.isBlank(text)) {
            return new Range<>(null, null);
        }
        String[] parts = text.split(",");
        if (parts.length == 1) {
            return new Range<>(integerParse(parts[0]), null);
        } else if (parts.length == 2) {
            return new Range<>(integerParse(parts[0]), integerParse(parts[1]));
        } else {
            throw new IllegalArgumentException(
                    String.format("Range must be in format '${min},${max}' but specified '%s'!", text));
        }
    }

    private static Integer integerParse(String text) {
        return StringUtils.isBlank(text) ? null : Integer.parseInt(text);
    }

    public T getStart() {
        return start;
    }

    public T getEnd() {
        return end;
    }

    public boolean contains(T value) {
        return (value.compareTo(start) >= 0) && (value.compareTo(end) <= 0);
    }

    @Override
    public String toString() {
        return String.format("Range[%s, %s]", start, end);
    }
}
