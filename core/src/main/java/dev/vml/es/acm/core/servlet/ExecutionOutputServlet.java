package dev.vml.es.acm.core.servlet;

import static dev.vml.es.acm.core.util.ServletResult.*;
import static dev.vml.es.acm.core.util.ServletUtils.*;

import dev.vml.es.acm.core.code.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.Servlet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
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
            ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + ExecutionOutputServlet.RT
        })
public class ExecutionOutputServlet extends SlingAllMethodsServlet {

    public static final String RT = "acm/api/execution-output";

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionOutputServlet.class);

    private static final String EXECUTION_ID_PARAM = "executionId";

    private static final String NAME_PARAM = "name";

    @Reference
    private transient ExecutionQueue queue;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String id = stringParam(request, EXECUTION_ID_PARAM);
        if (id == null) {
            respondJson(response, error(String.format("Execution ID is not specified!", EXECUTION_ID_PARAM)));
            return;
        }
        String name = stringParam(request, NAME_PARAM);
        if (name == null) {
            respondJson(response, error(String.format("Output name is not specified!", NAME_PARAM)));
            return;
        }

        try {
            ExecutionHistory executionHistory = new ExecutionHistory(request.getResourceResolver());
            Execution execution = executionHistory.findById(id).orElse(null);
            if (execution == null) {
                respondJson(response, error(String.format("Execution with id '%s' not found!", id)));
                return;
            }
            Output output = execution.getOutputs().stream().filter(o -> o.getName().equals(name)).findFirst().orElse(null);
            if (output == null) {
                respondJson(response, error(String.format("Execution output with name '%s' not found in execution '%s'!", name, id)));
                return;
            }   

            response.setContentType(output.getMimeType());
            response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", output.getDownloadName()));
            InputStream inputStream = executionHistory.readOutputByName(execution, name);
            IOUtils.copy(inputStream, response.getOutputStream());
        } catch (Exception e) {
            LOG.error("Execution output cannot be read for execution '{}' and name '{}'", id, name, e);
            respondJson(response, error(String.format("Execution output cannot be read for execution '%s' and name '%s'! Error: %s", id, name, e.getMessage()).trim()));
        }
    }
}
