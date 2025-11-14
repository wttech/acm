package dev.vml.es.acm.core.servlet;

import static dev.vml.es.acm.core.util.ServletResult.error;
import static dev.vml.es.acm.core.util.ServletResult.ok;
import static dev.vml.es.acm.core.util.ServletUtils.respondJson;

import dev.vml.es.acm.core.code.ExecutionQueue;
import dev.vml.es.acm.core.code.Executor;
import dev.vml.es.acm.core.gui.SpaSettings;
import dev.vml.es.acm.core.instance.HealthChecker;
import dev.vml.es.acm.core.instance.HealthStatus;
import dev.vml.es.acm.core.mock.MockHttpFilter;
import dev.vml.es.acm.core.mock.MockStatus;
import dev.vml.es.acm.core.osgi.InstanceInfo;
import dev.vml.es.acm.core.state.Permissions;
import dev.vml.es.acm.core.state.State;
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
            ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + StateServlet.RT,
            ServletResolverConstants.SLING_SERVLET_METHODS + "=GET",
            ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
        })
public class StateServlet extends SlingAllMethodsServlet {

    public static final String RT = "acm/api/state";

    private static final Logger LOG = LoggerFactory.getLogger(StateServlet.class);

    @Reference
    private transient ExecutionQueue executionQueue;

    @Reference
    private transient InstanceInfo instanceInfo;

    @Reference
    private transient HealthChecker healthChecker;

    @Reference
    private transient MockHttpFilter mockHttpFilter;

    @Reference
    private transient SpaSettings spaSettings;

    @Reference
    private transient Executor executor;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            HealthStatus healthStatus = healthChecker.checkStatus();
            MockStatus mockStatus = mockHttpFilter.checkStatus();
            Permissions permissions = new Permissions(request.getResourceResolver());
            State state = new State(spaSettings, healthStatus, mockStatus, instanceInfo.getSettings(), permissions);

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
