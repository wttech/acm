package com.wttech.aem.acm.core.servlet;

import static com.wttech.aem.acm.core.util.ServletResult.error;
import static com.wttech.aem.acm.core.util.ServletResult.ok;
import static com.wttech.aem.acm.core.util.ServletUtils.respondJson;

import com.wttech.aem.acm.core.code.Execution;
import com.wttech.aem.acm.core.code.ExecutionQueue;
import com.wttech.aem.acm.core.instance.HealthChecker;
import com.wttech.aem.acm.core.instance.HealthStatus;
import com.wttech.aem.acm.core.instance.InstanceSettings;
import com.wttech.aem.acm.core.osgi.OsgiContext;
import com.wttech.aem.acm.core.state.State;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.Servlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.settings.SlingSettingsService;
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
    private HealthChecker healthChecker;

    @Reference
    private ExecutionQueue executionQueue;

    @Reference
    private OsgiContext osgiContext;

    @Reference
    private SlingSettingsService slingSettingsService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            HealthStatus healthStatus = healthChecker.checkStatus();
            InstanceSettings instanceSettings = InstanceSettings.current();
            List<Execution> queuedExecutions = executionQueue.findAll().collect(Collectors.toList());
            boolean publish = osgiContext.getInstanceInfo().isPublish();
            // Version cloudVersion = osgiContext.getBundleContext()
            // LOG.info(cloudVersion.toString());

            State state = new State(healthStatus, instanceSettings, queuedExecutions, publish, false);

            // TODO use different view (skip outputs)
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
