package com.wttech.aem.acm.core.code;

import java.io.Serializable;
import java.util.Map;

public class Argument<V> implements Serializable {

    private final String name;

    private V value;

    private Map<String, Object> metadata;

    public Argument(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
