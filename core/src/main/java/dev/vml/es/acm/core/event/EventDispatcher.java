package dev.vml.es.acm.core.event;

import dev.vml.es.acm.core.code.ExecutionHistory;
import dev.vml.es.acm.core.code.ExecutionQueue;
import dev.vml.es.acm.core.util.ResourceUtils;
import java.util.Collections;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = {EventDispatcher.class, EventListener.class},
        immediate = true)
public class EventDispatcher implements EventListener {

    private final Logger LOG = LoggerFactory.getLogger(EventDispatcher.class);

    @Reference
    private EventManager eventManager;

    @Reference
    private ExecutionQueue executionQueue;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    public void dispatch(EventType eventType) {
        eventManager.triggerEvent(eventType.name().toLowerCase(), Collections.emptyMap());
    }

    @Override
    public void onEvent(Event event) {
        EventType eventType = EventType.of(event.getName()).orElse(null);
        if (eventType == null) {
            return;
        }
        switch (eventType) {
            case EXECUTION_QUEUE_RESET:
                executionQueue.reset();
                break;
            case HISTORY_CLEAR:
                doHistoryClear();
                break;
            default:
                // not handled
                break;
        }
    }

    private void doHistoryClear() {
        try (ResourceResolver resolver = ResourceUtils.contentResolver(resourceResolverFactory, null)) {
            ExecutionHistory executionHistory = new ExecutionHistory(resolver);
            executionHistory.clear();
        } catch (LoginException e) {
            LOG.error("Cannot access repository while clearing history!", e);
        }
    }
}
