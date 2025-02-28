package com.wttech.aem.contentor.core.servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;

import static com.wttech.aem.contentor.core.util.ServletResult.error;
import static com.wttech.aem.contentor.core.util.ServletResult.ok;
import static com.wttech.aem.contentor.core.util.ServletUtils.respondJson;

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

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            // TODO app state (health, settings, etc.)

            respondJson(response, ok("State read successfully", null));
        } catch (Exception e) {
            LOG.error("State cannot be read!", e);

            respondJson(response, error(String.format("State cannot be read! %s", e.getMessage()).trim()));
        }
    }
}
