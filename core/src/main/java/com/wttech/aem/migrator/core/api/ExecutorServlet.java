package com.wttech.aem.migrator.core.api;

import static com.wttech.aem.migrator.core.util.ServletUtils.*;
import static javax.servlet.http.HttpServletResponse.*;

import com.wttech.aem.migrator.core.script.Executor;
import com.wttech.aem.migrator.core.script.ScriptRepository;
import java.io.IOException;
import javax.servlet.Servlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        immediate = true,
        service = Servlet.class,
        property = {
            "sling.servlet.methods=GET",
            "sling.servlet.methods=POST",
            "sling.servlet.extensions=json",
            "sling.servlet.resourceTypes=" + ExecutorServlet.RT
        })
public class ExecutorServlet extends SlingAllMethodsServlet {

    public static final String RT = "migrator/api/executor";

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorServlet.class);

    private static final String PATH_PARAM = "path";

    @Reference
    private Executor executor;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.sendError(SC_OK, "This is executor servlet!");
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        var path = stringParam(request, PATH_PARAM);

        try {
            var script = new ScriptRepository(request.getResourceResolver())
                    .read(path)
                    .orElse(null);
            if (script == null) {
                respondJson(
                        response,
                        new Result(SC_BAD_REQUEST, String.format("Script '%s' to be executed not found", path)));
                ;
                return;
            }

            var execution = executor.execute(script);

            // TODO should this servlet also save execution in history?
            // history.save(execution)

            respondJson(response, new Result(SC_OK, "Execution result: " + execution.toString()));
        } catch (Exception e) {
            LOG.error("Cannot execute script at path '{}'", path, e);
            respondJson(response, new Result(SC_INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }
}
