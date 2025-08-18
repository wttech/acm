package dev.vml.es.acm.core.event;

import java.util.Calendar;

public interface Event {

    String getName();

    Calendar getTriggeredAt();
}
