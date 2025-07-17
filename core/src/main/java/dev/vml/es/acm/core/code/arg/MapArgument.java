package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.Argument;
import dev.vml.es.acm.core.code.ArgumentType;
import java.util.Map;

public class MapArgument extends Argument<Map<String, String>> {

    public MapArgument(String name) {
        super(name, ArgumentType.MAP, Map.class);
    }
}
