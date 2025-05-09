package com.vml.es.aem.acm.core.util;

import java.io.Serializable;

public class RangeArgumentObject<T extends Comparable<T>> implements Serializable {
    private T start;

    private T end;

    public RangeArgumentObject(T start, T end) {
        this.start = start;
        this.end = end;
    }

    public T getStart() {
        return start;
    }

    public void setStart(T start) {
        this.start = start;
    }

    public T getEnd() {
        return end;
    }

    public void setEnd(T end) {
        this.end = end;
    }
}
