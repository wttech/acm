package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.event.Event;
import dev.vml.es.acm.core.event.EventListener;
import dev.vml.es.acm.core.event.EventType;
import dev.vml.es.acm.core.util.ResolverUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = EventListener.class, immediate = true)
public class ExecutionHistoryManager implements EventListener {

    public static final String EXECUTABLE_ID_PROP = "executableId";

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionHistoryManager.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public void onEvent(Event event) {
        EventType eventType = EventType.of(event.getName()).orElse(null);
        if (eventType == EventType.HISTORY_CLEAR) {
            Object executableId = event.getProperties().get(EXECUTABLE_ID_PROP);
            clear(executableId == null ? null : executableId.toString());
        }
    }

    public void clear() {
        clear(null);
    }

    public void clear(String executableId) {
        try (ResourceResolver resolver = ResolverUtils.contentResolver(resourceResolverFactory, null)) {
            ExecutionHistory executionHistory = new ExecutionHistory(resolver);
            if (StringUtils.isNotBlank(executableId)) {
                ExecutionQuery query = new ExecutionQuery();
                query.setExecutableId(executableId);
                executionHistory.clear(query);
                LOG.info("Execution history cleared successfully for executable '{}'", executableId);
            } else {
                executionHistory.clear();
                LOG.info("Execution history cleared successfully");
            }
        } catch (LoginException e) {
            LOG.error("Cannot access repository while clearing history!", e);
        }
    }
}
