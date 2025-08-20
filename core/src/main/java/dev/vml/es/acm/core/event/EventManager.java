package dev.vml.es.acm.core.event;

import java.util.Map;

public interface EventManager {

    void triggerEvent(String name, Map<String, Object> properties);
}
