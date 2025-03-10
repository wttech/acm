package com.wttech.aem.contentor.core.servlet;

import static com.wttech.aem.contentor.core.util.ServletResult.*;
import static com.wttech.aem.contentor.core.util.ServletUtils.*;

import com.wttech.aem.contentor.core.code.*;
import com.wttech.aem.contentor.core.util.JsonUtils;
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

    public static final String RT = "contentor/api/queue-code";

    public static final String JOB_ID_PARAM = "jobId";

    private static final Logger LOG = LoggerFactory.getLogger(QueueCodeServlet.class);

    @Reference
    private ExecutionQueue executionQueue;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            ExecuteCodeInput input = JsonUtils.read(request.getInputStream(), ExecuteCodeInput.class);
            if (input == null) {
                respondJson(response, badRequest("Code input is not specified!"));
                return;
            }

            Code code = input.getCode();
            ExecutionContext context = executionQueue.createContext(code, request.getResourceResolver());
            if (input.getHistory() != null) {
                context.setHistory(input.getHistory());
            }

            ExecutionMode mode = ExecutionMode.of(input.getMode()).orElse(null);
            if (mode == null) {
                respondJson(response, badRequest(String.format("Execution mode '%s' is not supported!", mode)));
                return;
            }
            context.setMode(mode);

            Execution execution = executionQueue.submit(code).orElse(null);
            if (execution == null) {
                respondJson(response, error("Job cannot be queued!"));
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
                    badRequest(String.format("Job cannot be queued! %s", e.getMessage())
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
                            notFound(String.format("Job with ID '%s' not found!", StringUtils.join(jobIds, ","))));
                    return;
                }
            }

            QueueOutput output = new QueueOutput(executions);
            respondJson(response, ok("Job found successfully", output));
        } catch (Exception e) {
            LOG.error("Job cannot be read!", e);
            respondJson(
                    response,
                    error(String.format("Job cannot be read! %s", e.getMessage())
                            .trim()));
        }
    }

    @Override
    protected void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        List<String> jobIds = stringsParam(request, JOB_ID_PARAM);
        if (jobIds == null) {
            respondJson(response, badRequest("Job ID is not specified!"));
            return;
        }

        try {
            List<Execution> executions = executionQueue.readAll(jobIds).collect(Collectors.toList());
            if (executions.isEmpty()) {
                respondJson(
                        response,
                        notFound(String.format("Job with ID '%s' not found!", StringUtils.join(jobIds, ","))));
                return;
            }

            executions.forEach(e -> executionQueue.stop(e.getId()));

            QueueOutput output = new QueueOutput(executions);
            respondJson(response, ok("Job stopped successfully", output));
        } catch (Exception e) {
            LOG.error("Job cannot be stopped!", e);
            respondJson(
                    response,
                    error(String.format("Job cannot be stopped! %s", e.getMessage())
                            .trim()));
        }
    }
}
