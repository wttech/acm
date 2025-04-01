package com.wttech.aem.acm.core.servlet;

import static com.wttech.aem.acm.core.util.ServletResult.*;
import static com.wttech.aem.acm.core.util.ServletUtils.*;

import com.wttech.aem.acm.core.code.*;
import com.wttech.aem.acm.core.util.JsonUtils;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.Servlet;
import org.apache.commons.lang3.StringUtils;
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

    public static final String RT = "acm/api/queue-code";

    public static final String JOB_ID_PARAM = "jobId";

    private static final Logger LOG = LoggerFactory.getLogger(QueueCodeServlet.class);

    @Reference
    private ExecutionQueue executionQueue;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            QueueCodeInput input = JsonUtils.read(request.getInputStream(), QueueCodeInput.class);
            if (input == null) {
                respondJson(response, badRequest("Code input is not specified!"));
                return;
            }

            Code code = input.getCode();

            Execution execution = executionQueue.submit(code).orElse(null);
            if (execution == null) {
                respondJson(response, error("Code execution cannot be queued!"));
                return;
            }

            QueueOutput output = new QueueOutput(Collections.singletonList(execution));
            respondJson(
                    response,
                    ok(String.format("Code from '%s' queued for execution successfully", code.getId()), output));
        } catch (Exception e) {
            LOG.error("Job cannot be queued!", e);
            respondJson(
                    response,
                    badRequest(String.format("Code execution cannot be queued! %s", e.getMessage())
                            .trim()));
        }
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            List<String> jobIds = stringsParam(request, JOB_ID_PARAM);

            List<Execution> executions;
            if (jobIds == null) {
                executions = executionQueue.findAll().collect(Collectors.toList());
            } else {
                ExecutionResolver executionResolver =
                        new ExecutionResolver(executionQueue, request.getResourceResolver());
                executions = executionResolver.readAll(jobIds).collect(Collectors.toList());
                if (executions.isEmpty()) {
                    respondJson(
                            response,
                            notFound(String.format("Code execution with ID '%s' not found!", StringUtils.join(jobIds, ","))));
                    return;
                }
            }

            QueueOutput output = new QueueOutput(executions);
            respondJson(response, ok("Code execution found successfully", output));
        } catch (Exception e) {
            LOG.error("Job cannot be read!", e);
            respondJson(
                    response,
                    error(String.format("Code execution cannot be read! %s", e.getMessage())
                            .trim()));
        }
    }

    @Override
    protected void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        List<String> jobIds = stringsParam(request, JOB_ID_PARAM);
        if (jobIds == null) {
            respondJson(response, badRequest("Code execution ID is not specified!"));
            return;
        }

        try {
            List<Execution> executions = executionQueue.readAll(jobIds).collect(Collectors.toList());
            if (executions.isEmpty()) {
                respondJson(
                        response,
                        notFound(String.format("Code execution with ID '%s' not found!", StringUtils.join(jobIds, ","))));
                return;
            }

            executions.forEach(e -> executionQueue.stop(e.getId()));

            QueueOutput output = new QueueOutput(executions);
            respondJson(response, ok("Code execution stopped successfully", output));
        } catch (Exception e) {
            LOG.error("Code execution cannot be stopped!", e);
            respondJson(
                    response,
                    error(String.format("Code execution cannot be stopped! %s", e.getMessage())
                            .trim()));
        }
    }
}
