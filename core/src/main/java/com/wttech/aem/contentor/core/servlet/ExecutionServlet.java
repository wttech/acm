package com.wttech.aem.contentor.core.servlet;

import com.wttech.aem.contentor.core.code.Execution;
import com.wttech.aem.contentor.core.code.ExecutionHistory;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.wttech.aem.contentor.core.util.ServletResult.*;
import static com.wttech.aem.contentor.core.util.ServletUtils.*;

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

    private static final String ID_PARAM = "path";


    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            ExecutionHistory executionHistory = new ExecutionHistory(request.getResourceResolver());
            List<Execution> executions;

            List<String> ids = stringsParam(request, ID_PARAM);
            if (!ids.isEmpty()) {
                executions = executionHistory.readAll(ids).sorted().collect(Collectors.toList());
            } else {
                executions = executionHistory.findAll().sorted().collect(Collectors.toList());
            }
            ExecutionOutput output = new ExecutionOutput(executions);
            respondJson(response, ok("Executions listed successfully", output));
        } catch (Exception e) {
            LOG.error("Executions(s) cannot be read!", e);

            respondJson(response, error(String.format("Executions(s) cannot be read! %s", e.getMessage()).trim()));
        }
    }
}
