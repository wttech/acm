package dev.vml.es.acm.core.code.input;

import dev.vml.es.acm.core.code.InputType;
import java.util.Map;

public class MapInput extends AbstractKeyValueInput<Map<String, String>> {

    public MapInput(String name) {
        super(name, InputType.MAP, Map.class);
    }
}
