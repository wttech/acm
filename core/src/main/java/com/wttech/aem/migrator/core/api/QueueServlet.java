package com.wttech.aem.migrator.core.api;

import static com.wttech.aem.migrator.core.util.ServletUtils.*;
import static javax.servlet.http.HttpServletResponse.*;

import com.wttech.aem.migrator.core.script.ExecutionQueue;
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
            ServletResolverConstants.SLING_SERVLET_METHODS + "=GET",
            ServletResolverConstants.SLING_SERVLET_METHODS + "=POST",
            ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
            ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + QueueServlet.RT
        })
public class QueueServlet extends SlingAllMethodsServlet {

    public static final String RT = "migrator/api/queue";

    private static final Logger LOG = LoggerFactory.getLogger(QueueServlet.class);

    private static final String PATH_PARAM = "path";

    @Reference
    private ExecutionQueue queue;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        respondJson(response, new Result(SC_OK, "This is queue servlet!"));
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
                        new Result(SC_BAD_REQUEST, String.format("Script '%s' to be queued not found", path)));
                return;
            }

            queue.add(script);
        } catch (Exception e) {
            LOG.error("Cannot queue script at path '{}'", path, e);
            respondJson(response, new Result(SC_INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }
}
