package com.wttech.aem.migrator.core.script;

import java.io.IOException;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
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
            "sling.servlet.resourceTypes=" + QueueServlet.RT
        })
public class QueueServlet extends SlingAllMethodsServlet {

    public static final String RT = "migrator/api/queue";

    private static final Logger LOG = LoggerFactory.getLogger(QueueServlet.class);

    private static final String PATH_PARAM = "path";

    @Reference
    private ExecutionQueue queue;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_OK, "This is queue servlet!");
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        var path = request.getParameter(PATH_PARAM);

        try {
            var script = new ScriptRepository(request.getResourceResolver())
                    .read(path)
                    .orElse(null);
            if (script == null) {
                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST, String.format("Script '%s' to be queued not found", path));
                return;
            }

            queue.add(script);
        } catch (Exception e) {
            LOG.error("Cannot queue script at path '{}'", path, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
