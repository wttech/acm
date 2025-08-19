package dev.vml.es.acm.core.servlet;

import static dev.vml.es.acm.core.util.ServletResult.*;
import static dev.vml.es.acm.core.util.ServletUtils.respondJson;

import dev.vml.es.acm.core.event.EventDispatcher;
import dev.vml.es.acm.core.event.EventType;
import java.io.IOException;
import javax.servlet.Servlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = {Servlet.class},
        property = {
            ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + EventServlet.RT,
            ServletResolverConstants.SLING_SERVLET_METHODS + "=POST",
            ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
        })
public class EventServlet extends SlingAllMethodsServlet {

    public static final String RT = "acm/api/event";

    public static final String NAME_PARAM = "name";

    private static final Logger LOG = LoggerFactory.getLogger(EventServlet.class);

    @Reference
    private EventDispatcher dispatcher;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String name = request.getParameter(NAME_PARAM);
        EventType event = EventType.of(name).orElse(null);
        if (event == null) {
            respondJson(response, badRequest(String.format("Event '%s' is not supported!", name)));
            return;
        }

        try {
            dispatcher.dispatch(event);
            respondJson(response, ok(String.format("Event '%s' dispatched successfully!", name)));
        } catch (Exception e) {
            LOG.error("Event '{}' cannot be dispatched!", name, e);
            respondJson(
                    response,
                    badRequest(String.format("Event '%s' cannot be dispatched! %s", name, e.getMessage())
                            .trim()));
        }
    }
}
