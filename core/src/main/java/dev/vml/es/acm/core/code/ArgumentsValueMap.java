package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.repo.RepoValueMap;
import java.util.Map;

public class ArgumentsValueMap extends RepoValueMap {

    private final Arguments arguments;

    public ArgumentsValueMap(Arguments arguments, Map<String, Object> base) {
        super(base);
        this.arguments = arguments;
    }

    /**
     * Auto-convert the value to the type of the argument if it's known.
     */
    @Override
    public Object get(Object name) {
        String key = (String) name;
        Class<?> type = arguments.get(key).getValueType();
        if (type == null) {
            return super.get(name);
        }
        return super.get(key, type);
    }
}
