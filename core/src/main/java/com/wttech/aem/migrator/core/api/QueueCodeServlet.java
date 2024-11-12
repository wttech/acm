package com.wttech.aem.migrator.core.api;

import static com.wttech.aem.migrator.core.util.ServletUtils.respondJson;
import static javax.servlet.http.HttpServletResponse.*;

import com.wttech.aem.migrator.core.script.ExecutionMode;
import com.wttech.aem.migrator.core.script.ExecutionOptions;
import com.wttech.aem.migrator.core.script.ExecutionQueue;
import com.wttech.aem.migrator.core.util.JsonUtils;
import com.wttech.aem.migrator.core.util.ServletUtils;
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
        service = {Servlet.class},
        property = {
            ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + QueueCodeServlet.RT,
            ServletResolverConstants.SLING_SERVLET_METHODS + "=GET",
            ServletResolverConstants.SLING_SERVLET_METHODS + "=POST",
            ServletResolverConstants.SLING_SERVLET_METHODS + "=DELETE",
            ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
        })
public class QueueCodeServlet extends SlingAllMethodsServlet {

    public static final String RT = "migrator/api/queue-code";

    public static final String JOB_ID_PARAM = "jobId";

    private static final Logger LOG = LoggerFactory.getLogger(QueueCodeServlet.class);

    @Reference
    private ExecutionQueue executionQueue;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            var input = JsonUtils.readJson(request.getInputStream(), ExecuteCodeInput.class);
            if (input == null) {
                respondJson(response, new Result(SC_BAD_REQUEST, "Code input is not specified!"));
                return;
            }

            var code = input.getCode();
            var options = new ExecutionOptions(request.getResourceResolver());

            var mode = ExecutionMode.of(input.getMode()).orElse(null);
            if (mode == null) {
                respondJson(
                        response,
                        new Result(SC_BAD_REQUEST, String.format("Execution mode '%s' is not supported!", mode)));
                return;
            }
            options.setMode(mode);

            var job = executionQueue.add(code).orElse(null);
            if (job == null) {
                respondJson(response, new Result(SC_INTERNAL_SERVER_ERROR, "Code cannot be queued for execution!"));
                return;
            }

            respondJson(
                    response,
                    new Result(
                            SC_OK,
                            String.format("Code from '%s' queued for execution successfully", code.getId()),
                            job));
        } catch (Exception e) {
            LOG.error("Code input cannot be read!", e);
            respondJson(
                    response,
                    new Result(
                            SC_BAD_REQUEST,
                            String.format("Code input cannot be read! %s", e.getMessage())
                                    .trim()));
        }
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        var jobId = ServletUtils.stringParam(request, JOB_ID_PARAM);
        if (jobId == null) {
            ServletUtils.respondJson(response, new Result(SC_BAD_REQUEST, "Job ID is not specified!"));
            return;
        }

        var job = executionQueue.find(jobId).orElse(null);
        if (job == null) {
            ServletUtils.respondJson(
                    response, new Result(SC_NOT_FOUND, String.format("Job with ID '%s' not found!", jobId)));
            return;
        }

        ServletUtils.respondJson(response, new Result(SC_OK, "Job found successfully", job));
    }

    @Override
    protected void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        var jobId = ServletUtils.stringParam(request, JOB_ID_PARAM);
        if (jobId == null) {
            ServletUtils.respondJson(response, new Result(SC_BAD_REQUEST, "Job ID is not specified!"));
            return;
        }

        var job = executionQueue.find(jobId).orElse(null);
        if (job == null) {
            ServletUtils.respondJson(
                    response, new Result(SC_NOT_FOUND, String.format("Job with ID '%s' not found!", jobId)));
            return;
        }

        executionQueue.stop(jobId);

        ServletUtils.respondJson(response, new Result(SC_OK, "Job removed successfully", job));
    }
}
