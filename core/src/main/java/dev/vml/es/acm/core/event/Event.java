package dev.vml.es.acm.core.event;

import java.util.Calendar;
import java.util.Map;

public interface Event {

    String getName();

    Map<String, Object> getProperties();

    Calendar getTriggeredAt();
}
