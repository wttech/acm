package com.wttech.aem.contentor.core.servlet;

import static com.wttech.aem.contentor.core.util.ServletResult.*;
import static com.wttech.aem.contentor.core.util.ServletUtils.*;

import com.wttech.aem.contentor.core.code.*;
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
        immediate = true,
        service = Servlet.class,
        property = {
            ServletResolverConstants.SLING_SERVLET_METHODS + "=GET",
            ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
            ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + ExecutionServlet.RT
        })
public class ExecutionServlet extends SlingAllMethodsServlet {

    public static final String RT = "contentor/api/execution";

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionServlet.class);

    private static final String ID_PARAM = "id";

    @Reference
    private ExecutionQueue queue;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            ExecutionHistory executionHistory = new ExecutionHistory(request.getResourceResolver());
            List<Execution> executions;

            List<String> ids = stringsParam(request, ID_PARAM);
            if (ids != null) {
                ExecutionResolver executionResolver = new ExecutionResolver(queue, request.getResourceResolver());
                executions = executionResolver.readAll(ids).collect(Collectors.toList());
            } else {
                ExecutionQuery criteria = ExecutionQuery.from(request);
                executions = executionHistory.findAll(criteria).collect(Collectors.toList());
            }

            ExecutionOutput output = new ExecutionOutput(executions);
            respondJson(response, ok("Executions listed successfully", output));
        } catch (Exception e) {
            LOG.error("Executions(s) cannot be read!", e);

            respondJson(
                    response,
                    error(String.format("Executions(s) cannot be read! %s", e.getMessage())
                            .trim()));
        }
    }
}
