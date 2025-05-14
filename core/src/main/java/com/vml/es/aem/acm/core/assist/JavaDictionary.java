package com.vml.es.aem.acm.core.assist;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaDictionary {

    private final Map<String, List<String>> modules = new HashMap<>();

    @JsonAnyGetter
    public Map<String, List<String>> getModules() {
        return modules;
    }

    @JsonAnySetter
    public void setModule(String key, List<String> value) {
        modules.put(key, value);
    }
}
