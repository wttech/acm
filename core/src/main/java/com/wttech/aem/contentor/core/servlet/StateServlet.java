package com.wttech.aem.contentor.core.servlet;

import static com.wttech.aem.contentor.core.util.ServletResult.error;
import static com.wttech.aem.contentor.core.util.ServletResult.ok;
import static com.wttech.aem.contentor.core.util.ServletUtils.respondJson;

import com.wttech.aem.contentor.core.instance.HealthChecker;
import com.wttech.aem.contentor.core.instance.HealthStatus;
import com.wttech.aem.contentor.core.instance.InstanceSettings;
import com.wttech.aem.contentor.core.state.State;
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

    public static final String RT = "contentor/api/state";

    private static final Logger LOG = LoggerFactory.getLogger(StateServlet.class);

    @Reference
    private HealthChecker healthChecker;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            HealthStatus healthStatus = healthChecker.checkStatus();
            InstanceSettings instanceSettings = InstanceSettings.current();
            State state = new State(healthStatus, instanceSettings);

            respondJson(response, ok("State read successfully", state));
        } catch (Exception e) {
            LOG.error("State cannot be read!", e);
            respondJson(
                    response,
                    error(String.format("State cannot be read! %s", e.getMessage())
                            .trim()));
        }
    }
}
