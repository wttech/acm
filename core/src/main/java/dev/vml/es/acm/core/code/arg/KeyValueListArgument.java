package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.ArgumentType;
import dev.vml.es.acm.core.util.KeyValue;
import dev.vml.es.acm.core.util.KeyValueList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KeyValueListArgument extends AbstractKeyValueArgument<KeyValueList<String, String>> {

    public KeyValueListArgument(String name) {
        super(name, ArgumentType.KEY_VALUE_LIST, null);
    }

    public void setValue(Map<String, String> value) {
        if (value == null) {
            super.setValue(null);
        } else {
            super.setValue(value.entrySet().stream()
                    .map(entry -> new KeyValue<>(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toCollection(KeyValueList::new)));
        }
    }

    @SuppressWarnings("unchecked")
    public void setValue(List<Object> value) {
        if (value == null) {
            super.setValue(null);
            return;
        }
        KeyValueList<String, String> newList = new KeyValueList<>();
        for (Object v : value) {
            if (v instanceof List) {
                List<String> vList = (List<String>) v;
                if (vList.size() != 2) {
                    throw new IllegalArgumentException("Key-value list must contain pairs of key and value!");
                }
                newList.add(new KeyValue<>(vList.get(0), vList.get(1)));
            } else if (v instanceof Map) {
                Map<String, String> vMap = (Map<String, String>) v;
                if (vMap.size() != 1) {
                    throw new IllegalArgumentException("Key-value map must contain exactly one key-value pair!");
                }
                String key = vMap.keySet().iterator().next();
                String valueStr = vMap.get(key);
                newList.add(new KeyValue<>(key, valueStr));
            } else if (v instanceof KeyValue) {
                KeyValue<String, String> kv = (KeyValue<String, String>) v;
                newList.add(kv);
            } else {
                throw new IllegalArgumentException(String.format(
                        "Key-value list element type '%s' is not supported!",
                        v.getClass().getName()));
            }
        }
        super.setValue(newList);
    }
}
