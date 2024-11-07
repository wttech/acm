package com.wttech.aem.migrator.core.api;

import static com.wttech.aem.migrator.core.util.ServletUtils.*;
import static javax.servlet.http.HttpServletResponse.*;

import com.wttech.aem.migrator.core.script.ExecutionOptions;
import com.wttech.aem.migrator.core.script.Executor;
import com.wttech.aem.migrator.core.script.ScriptRepository;
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

    public static final String RT = "migrator/api/execute-script";

    private static final Logger LOG = LoggerFactory.getLogger(ExecuteScriptServlet.class);

    private static final String PATH_PARAM = "path";

    @Reference
    private Executor executor;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        var path = stringParam(request, PATH_PARAM);

        try {
            var script = new ScriptRepository(request.getResourceResolver())
                    .read(path)
                    .orElse(null);
            if (script == null) {
                respondJson(
                        response, new Result(SC_BAD_REQUEST, String.format("Script at path '%s' not found!", path)));
                return;
            }

            var options = new ExecutionOptions(request.getResourceResolver());
            var execution = executor.execute(script, options);

            // TODO should this servlet also save execution in history?
            // history.save(execution)

            respondJson(
                    response,
                    new Result(SC_OK, String.format("Script at path '%s' executed successfully", path), execution));
        } catch (Exception e) {
            LOG.error("Cannot execute script at path '{}'", path, e);
            respondJson(
                    response,
                    new Result(
                            SC_INTERNAL_SERVER_ERROR,
                            String.format("Script at path '%s' cannot be executed. Error: %s", path, e.getMessage())));
        }
    }
}
