package dev.vml.es.acm.core.servlet;

import static dev.vml.es.acm.core.util.ServletResult.error;
import static dev.vml.es.acm.core.util.ServletResult.ok;
import static dev.vml.es.acm.core.util.ServletUtils.respondJson;

import dev.vml.es.acm.core.code.ExecutionQueue;
import dev.vml.es.acm.core.code.ExecutionSummary;
import dev.vml.es.acm.core.gui.SpaSettings;
import dev.vml.es.acm.core.instance.HealthChecker;
import dev.vml.es.acm.core.instance.HealthStatus;
import dev.vml.es.acm.core.mock.MockHttpFilter;
import dev.vml.es.acm.core.mock.MockStatus;
import dev.vml.es.acm.core.osgi.InstanceInfo;
import dev.vml.es.acm.core.state.State;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
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
            ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + StateServlet.RT,
            ServletResolverConstants.SLING_SERVLET_METHODS + "=GET",
            ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
        })
public class StateServlet extends SlingAllMethodsServlet {

    public static final String RT = "acm/api/state";

    private static final Logger LOG = LoggerFactory.getLogger(StateServlet.class);

    @Reference
    private ExecutionQueue executionQueue;

    @Reference
    private InstanceInfo instanceInfo;

    @Reference
    private HealthChecker healthChecker;

    @Reference
    private MockHttpFilter mockHttpFilter;

    @Reference
    private SpaSettings spaSettings;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            HealthStatus healthStatus = healthChecker.checkStatus();
            MockStatus mockStatus = mockHttpFilter.checkStatus();
            List<ExecutionSummary> queuedExecutions =
                    executionQueue.findAllSummaries().collect(Collectors.toList());
            State state = new State(spaSettings, healthStatus, mockStatus, instanceInfo.getSettings());

            respondJson(response, ok("State read successfully", state));
        } catch (Exception e) {
            LOG.error("State cannot be read!", e);
            respondJson(
                    response,
                    error(String.format(
                            "State cannot be read! %s", e.getMessage().trim())));
        }
    }
}
