package com.wttech.aem.migrator.core.api;

import static com.wttech.aem.migrator.core.util.ServletUtils.*;
import static com.wttech.aem.migrator.core.util.ServletUtils.respondJson;
import static javax.servlet.http.HttpServletResponse.*;

import com.wttech.aem.migrator.core.script.*;
import com.wttech.aem.migrator.core.util.JsonUtils;
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
            ExecuteCodeInput input = JsonUtils.readJson(request.getInputStream(), ExecuteCodeInput.class);
            if (input == null) {
                respondJson(response, Result.badRequest("Code input is not specified!"));
                return;
            }

            Code code = input.getCode();
            ExecutionOptions options = new ExecutionOptions(request.getResourceResolver());

            ExecutionMode mode = ExecutionMode.of(input.getMode()).orElse(null);
            if (mode == null) {
                respondJson(response, Result.badRequest(String.format("Execution mode '%s' is not supported!", mode)));
                return;
            }
            options.setMode(mode);

            Execution execution = executionQueue.submit(code).orElse(null);
            if (execution == null) {
                respondJson(response, Result.error("Code cannot be queued for execution!"));
                return;
            }

            respondJson(
                    response,
                    new Result(
                            SC_OK,
                            String.format("Code from '%s' queued for execution successfully", code.getId()),
                            execution));
        } catch (Exception e) {
            LOG.error("Code input cannot be read!", e);
            respondJson(
                    response,
                    Result.badRequest(String.format("Code input cannot be read! %s", e.getMessage())
                            .trim()));
        }
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String jobId = stringParam(request, JOB_ID_PARAM);
        if (jobId == null) {
            respondJson(response, Result.badRequest("Job ID is not specified!"));
            return;
        }

        try {
            Execution execution = executionQueue.read(jobId).orElse(null);
            if (execution == null) {
                respondJson(response, Result.notFound(String.format("Job with ID '%s' not found!", jobId)));
                return;
            }

            respondJson(response, Result.ok("Job found successfully", execution));
        } catch (Exception e) {
            LOG.error("Job cannot be read!", e);
            respondJson(
                    response,
                    Result.error(String.format("Job cannot be read! %s", e.getMessage())
                            .trim()));
        }
    }

    @Override
    protected void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String jobId = stringParam(request, JOB_ID_PARAM);
        if (jobId == null) {
            respondJson(response, Result.badRequest("Job ID is not specified!"));
            return;
        }

        try {
            Execution execution = executionQueue.read(jobId).orElse(null);
            if (execution == null) {
                respondJson(response, Result.notFound(String.format("Job with ID '%s' not found!", jobId)));
                return;
            }

            executionQueue.stop(jobId);

            respondJson(response, Result.ok("Job stopped successfully", execution));
        } catch (Exception e) {
            LOG.error("Job cannot be stopped!", e);
            respondJson(
                    response,
                    Result.error(String.format("Job cannot be stopped! %s", e.getMessage())
                            .trim()));
        }
    }
}
