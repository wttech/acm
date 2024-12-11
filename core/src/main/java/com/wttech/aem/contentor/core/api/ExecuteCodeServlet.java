package com.wttech.aem.contentor.core.api;

import com.wttech.aem.contentor.core.code.*;
import com.wttech.aem.contentor.core.util.JsonUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;

import static com.wttech.aem.contentor.core.util.ServletResult.*;
import static com.wttech.aem.contentor.core.util.ServletUtils.respondJson;

@Component(
        immediate = true,
        service = Servlet.class,
        property = {
                ServletResolverConstants.SLING_SERVLET_METHODS + "=POST",
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
                ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + ExecuteCodeServlet.RT
        })
public class ExecuteCodeServlet extends SlingAllMethodsServlet {

    public static final String RT = "contentor/api/execute-code";

    private static final Logger LOG = LoggerFactory.getLogger(ExecuteCodeServlet.class);

    @Reference
    private Executor executor;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            ExecuteCodeInput input = JsonUtils.readJson(request.getInputStream(), ExecuteCodeInput.class);
            if (input == null) {
                respondJson(response, badRequest("Code input is not specified!"));
                return;
            }

            Code code = input.getCode();
            ExecutionContext options = executor.createContext(code, request.getResourceResolver());

            ExecutionMode mode = ExecutionMode.of(input.getMode()).orElse(null);
            if (mode == null) {
                respondJson(response, badRequest(String.format("Execution mode '%s' is not supported!", input.getMode())));
                return;
            }
            options.setMode(mode);

            try {
                Execution execution = executor.execute(code, options);

                // TODO should this servlet also save execution in history?
                // history.save(execution)

                respondJson(response, ok(String.format("Code from '%s' executed successfully", code.getId()), execution));
            } catch (Exception e) {
                LOG.error("Code from '{}' cannot be executed!", code.getId(), e);
                respondJson(response, error(String.format("Code from '%s' cannot be executed. Error: %s", code.getId(), e.getMessage())));
            }
            respondJson(response, ok("Code executed successfully"));
        } catch (Exception e) {
            LOG.error("Code input cannot be read!", e);
            respondJson(response, badRequest("Cannot read code input!"));
            return;
        }
    }
}
