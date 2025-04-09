package com.wttech.aem.acm.core.servlet;

import static com.wttech.aem.acm.core.util.ServletResult.*;
import static com.wttech.aem.acm.core.util.ServletUtils.respondJson;
import static com.wttech.aem.acm.core.util.ServletUtils.stringParam;

import com.wttech.aem.acm.core.code.Execution;
import com.wttech.aem.acm.core.code.ExecutionContext;
import com.wttech.aem.acm.core.code.ExecutionId;
import com.wttech.aem.acm.core.code.Executor;
import com.wttech.aem.acm.core.script.Script;
import com.wttech.aem.acm.core.script.ScriptRepository;
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
            ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + ExecuteScriptServlet.RT
        })
public class ExecuteScriptServlet extends SlingAllMethodsServlet {

    public static final String RT = "acm/api/execute-script";

    private static final Logger LOG = LoggerFactory.getLogger(ExecuteScriptServlet.class);

    private static final String PATH_PARAM = "path";

    @Reference
    private Executor executor;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String path = stringParam(request, PATH_PARAM);

        try {
            Script script = new ScriptRepository(request.getResourceResolver())
                    .read(path)
                    .orElse(null);
            if (script == null) {
                respondJson(response, badRequest(String.format("Script at path '%s' not found!", path)));
                return;
            }

            try (ExecutionContext context =
                    executor.createContext(ExecutionId.generate(), script, request.getResourceResolver())) {
                Execution execution = executor.execute(context);

                respondJson(response, ok(String.format("Script at path '%s' executed successfully", path), execution));
            }
        } catch (Exception e) {
            LOG.error("Cannot execute script at path '{}'", path, e);
            respondJson(
                    response,
                    error(String.format("Script at path '%s' cannot be executed. Error: %s", path, e.getMessage())));
        }
    }
}
