package dev.vml.es.acm.core.repo;

import java.util.Map;
import java.util.Optional;

import dev.vml.es.acm.core.util.TypeConverter;
import org.apache.sling.api.wrappers.ValueMapDecorator;

public class RepoValueMap extends ValueMapDecorator {

    public RepoValueMap(Map<String, Object> base) {
        super(base);
    }

    @Override
    public <T> T get(String name, Class<T> type) {
        return Optional.ofNullable(super.get(name))
                .flatMap(v -> TypeConverter.convert(v, type))
                .orElseGet(() -> super.get(name, type));
    }
}
