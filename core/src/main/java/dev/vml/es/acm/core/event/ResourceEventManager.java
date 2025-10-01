package dev.vml.es.acm.core.event;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.util.ResolverUtils;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import javax.jcr.Session;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = {EventManager.class, ResourceChangeListener.class},
        immediate = true,
        property = {
            ResourceChangeListener.PATHS + "=glob:" + ResourceEvent.ROOT + "/**/*",
            ResourceChangeListener.CHANGES + "=ADDED",
            ResourceChangeListener.CHANGES + "=CHANGED",
        })
public class ResourceEventManager implements EventManager, ResourceChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceEventManager.class);

    @Reference
    private Replicator replicator;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            service = EventListener.class)
    private final Collection<EventListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void triggerEvent(String name, Map<String, Object> properties) {
        try (ResourceResolver resourceResolver = ResolverUtils.contentResolver(resourceResolverFactory, null)) {
            LOG.debug("Triggering event: {}", name);
            ResourceEvent event = ResourceEvent.create(name, properties, resourceResolver);
            replicator.replicate(
                    resourceResolver.adaptTo(Session.class), ReplicationActionType.ACTIVATE, event.getPath());
            LOG.debug("Triggered event: {}", event);
        } catch (LoginException e) {
            throw new AcmException(String.format("Cannot access repository while triggering event '%s'!", name), e);
        } catch (ReplicationException e) {
            throw new AcmException(String.format("Cannot replicate event '%s'!", name), e);
        }
    }

    @Override
    public void onChange(@NotNull List<ResourceChange> changes) {
        if ((changes == null || changes.isEmpty()) || listeners.isEmpty()) {
            return;
        }
        try (ResourceResolver resourceResolver = ResolverUtils.contentResolver(resourceResolverFactory, null)) {
            Collection<Event> events = changes.stream()
                    .map(c -> resourceResolver.getResource(c.getPath()))
                    .filter(Objects::nonNull)
                    .map(ResourceEvent::new)
                    .collect(Collectors.toList());

            events.forEach(event -> {
                LOG.debug("Dispatching event to listeners ({}): {}", listeners.size(), event);
                listeners.forEach(listener -> {
                    try {
                        listener.onEvent(event);
                    } catch (Exception e) {
                        LOG.error(
                                "Event listener '{}' cannot handle event properly!",
                                listener.getClass().getName(),
                                e);
                    }
                });
                LOG.debug("Dispatched event to listeners ({}): {}", listeners.size(), event);
            });
        } catch (LoginException e) {
            throw new AcmException("Cannot access repository while processing events!", e);
        }
    }
}
