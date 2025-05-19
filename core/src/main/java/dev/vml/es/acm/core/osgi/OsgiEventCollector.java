package dev.vml.es.acm.core.osgi;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import org.osgi.service.event.Event;

public class OsgiEventCollector {

    private final LinkedBlockingQueue<OsgiEvent> events;

    public OsgiEventCollector(int maxSize) {
        this.events = new LinkedBlockingQueue<>(maxSize);
    }

    public void addEvent(Event event) {
        long received = System.currentTimeMillis();
        events.offer(new OsgiEvent(event.getTopic(), received));
    }

    public List<OsgiEvent> getRecentEvents(long timeWindow) {
        long currentTime = System.currentTimeMillis();
        long cutoffTime = currentTime - timeWindow;

        return events.stream()
                .filter(event -> event.getReceived() >= cutoffTime)
                .collect(Collectors.toList());
    }
}
