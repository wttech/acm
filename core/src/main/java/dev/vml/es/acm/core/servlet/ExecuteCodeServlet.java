package dev.vml.es.acm.core.servlet;

import static dev.vml.es.acm.core.util.ServletResult.*;
import static dev.vml.es.acm.core.util.ServletUtils.respondJson;

import dev.vml.es.acm.core.code.*;
import dev.vml.es.acm.core.util.JsonUtils;
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
        immediate = true,
        service = Servlet.class,
        property = {
            ServletResolverConstants.SLING_SERVLET_METHODS + "=POST",
            ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
            ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + ExecuteCodeServlet.RT
        })
public class ExecuteCodeServlet extends SlingAllMethodsServlet {

    public static final String RT = "acm/api/execute-code";

    private static final Logger LOG = LoggerFactory.getLogger(ExecuteCodeServlet.class);

    @Reference
    private transient Executor executor;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        ExecuteCodeInput input;
        try {
            input = JsonUtils.read(request.getInputStream(), ExecuteCodeInput.class);
        } catch (Exception e) {
            LOG.error("Code input cannot be read!", e);
            respondJson(response, badRequest("Cannot read code input!"));
            return;
        }
        if (input == null) {
            respondJson(response, badRequest("Code input is not specified!"));
            return;
        }

        Code code = input.getCode();

        ExecutionMode mode = ExecutionMode.of(input.getMode()).orElse(null);
        if (mode == null) {
            respondJson(response, badRequest(String.format("Execution mode '%s' is not supported!", input.getMode())));
            return;
        }

        try (ExecutionContext context = executor.createContext(
                ExecutionId.generate(),
                request.getResourceResolver().getUserID(),
                mode,
                code,
                input.getInputs(),
                request.getResourceResolver())) {
            if (input.getHistory() != null) {
                context.setHistory(input.getHistory());
            }

            try {
                Execution execution = executor.execute(context);

                respondJson(
                        response, ok(String.format("Code from '%s' executed successfully", code.getId()), execution));
            } catch (Exception e) {
                LOG.error("Code from '{}' cannot be executed!", code.getId(), e);
                respondJson(
                        response,
                        error(String.format(
                                "Code from '%s' cannot be executed. Error: %s", code.getId(), e.getMessage())));
            }
        }
    }
}
