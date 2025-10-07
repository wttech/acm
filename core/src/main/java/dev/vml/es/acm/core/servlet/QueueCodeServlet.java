package dev.vml.es.acm.core.servlet;

import static dev.vml.es.acm.core.util.ServletResult.*;
import static dev.vml.es.acm.core.util.ServletUtils.*;

import dev.vml.es.acm.core.code.*;
import dev.vml.es.acm.core.servlet.input.QueueCodeInput;
import dev.vml.es.acm.core.servlet.output.QueueOutput;
import dev.vml.es.acm.core.util.JsonUtils;
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

    public static final String EXECUTION_ID_PARAM = "executionId";

    private static final Logger LOG = LoggerFactory.getLogger(QueueCodeServlet.class);

    @Reference
    private transient ExecutionQueue executionQueue;

    @Reference
    private transient Executor executor;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            QueueCodeInput input = JsonUtils.read(request.getInputStream(), QueueCodeInput.class);
            if (input == null) {
                respondJson(response, badRequest("Code input is not specified!"));
                return;
            }

            Code code = input.getCode();

            try (ExecutionContext context = executor.createContext(
                    ExecutionId.generate(),
                    request.getResourceResolver().getUserID(),
                    ExecutionMode.CHECK,
                    input.getCode(),
                    input.getInputs(),
                    request.getResourceResolver(),
                    new CodeOutputMemory())) {
                Execution checkExecution = executor.execute(context);
                if (checkExecution.getStatus() == ExecutionStatus.SKIPPED) {
                    QueueOutput output = new QueueOutput(Collections.singletonList(checkExecution));
                    respondJson(response, ok(String.format("Code from '%s' skipped execution", code.getId()), output));
                    return;
                }
            } catch (Exception e) {
                LOG.error("Code execution cannot be checked!", e);
                respondJson(
                        response,
                        badRequest(String.format("Code execution cannot be checked! %s", e.getMessage())
                                .trim()));
                return;
            }

            ExecutionContextOptions contextOptions = new ExecutionContextOptions(
                    ExecutionMode.RUN, request.getResourceResolver().getUserID(), input.getInputs());

            Execution execution = executionQueue.submit(code, contextOptions);
            QueueOutput output = new QueueOutput(Collections.singletonList(execution));
            respondJson(
                    response,
                    ok(String.format("Code from '%s' queued for execution successfully", code.getId()), output));
        } catch (Exception e) {
            LOG.error("Execution cannot be queued!", e);
            respondJson(
                    response,
                    badRequest(String.format("Code execution cannot be queued! %s", e.getMessage())
                            .trim()));
        }
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            List<String> executionIds = stringsParam(request, EXECUTION_ID_PARAM);

            List<Execution> executions;
            if (executionIds == null) {
                executions = executionQueue.findAll().collect(Collectors.toList());
            } else {
                ExecutionResolver executionResolver =
                        new ExecutionResolver(executionQueue, request.getResourceResolver());
                executions = executionResolver.resolveAll(executionIds).collect(Collectors.toList());
                if (executions.isEmpty()) {
                    respondJson(
                            response,
                            notFound(String.format(
                                    "Code execution with ID '%s' not found!", StringUtils.join(executionIds, ","))));
                    return;
                }
            }

            QueueOutput output = new QueueOutput(executions);
            respondJson(response, ok("Code execution found successfully", output));
        } catch (Exception e) {
            LOG.error("Execution cannot be read!", e);
            respondJson(
                    response,
                    error(String.format("Code execution cannot be read! %s", e.getMessage())
                            .trim()));
        }
    }

    @Override
    protected void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        List<String> executionIds = stringsParam(request, EXECUTION_ID_PARAM);
        if (executionIds == null) {
            respondJson(response, badRequest("Code execution ID is not specified!"));
            return;
        }

        try {
            List<Execution> executions = executionQueue.readAll(executionIds).collect(Collectors.toList());
            if (executions.isEmpty()) {
                respondJson(
                        response,
                        notFound(String.format(
                                "Code execution with ID '%s' not found!", StringUtils.join(executionIds, ","))));
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
