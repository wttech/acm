package dev.vml.es.acm.core.servlet;

import static dev.vml.es.acm.core.util.ServletResult.*;
import static dev.vml.es.acm.core.util.ServletUtils.*;

import dev.vml.es.acm.core.code.*;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.Servlet;
import org.apache.commons.lang3.BooleanUtils;
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
            ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
            ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + ExecutionServlet.RT
        })
public class ExecutionServlet extends SlingAllMethodsServlet {

    public static final String RT = "acm/api/execution";

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionServlet.class);

    private static final String ID_PARAM = "id";

    private static final String FORMAT_PARAM = "format";

    private static final String LIMIT_PARAM = "limit";

    private static final String OFFSET_PARAM = "offset";

    private static final String QUEUED_PARAM = "queued";

    @Reference
    private transient ExecutionQueue queue;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String formatParam = request.getParameter(FORMAT_PARAM);
        ExecutionFormat format = ExecutionFormat.of(formatParam).orElse(null);
        if (format == null) {
            respondJson(response, error(String.format("Execution format '%s' is not supported!", formatParam)));
            return;
        }

        Stream<?> executionStream;
        try {
            ExecutionHistory executionHistory = new ExecutionHistory(request.getResourceResolver());

            List<String> ids = stringsParam(request, ID_PARAM);
            if (ids != null) {
                ExecutionResolver executionResolver = new ExecutionResolver(queue, request.getResourceResolver());
                if (format == ExecutionFormat.FULL) {
                    executionStream = executionResolver.resolveAll(ids);
                } else {
                    executionStream = executionResolver.resolveAllSummaries(ids);
                }
            } else {
                ExecutionQuery criteria = ExecutionQuery.from(request);
                if (format == ExecutionFormat.FULL) {
                    executionStream = executionHistory.findAll(criteria);
                } else {
                    Boolean queued = boolParam(request, QUEUED_PARAM);
                    if (BooleanUtils.isTrue(queued)) {
                        executionStream = queue.findAllSummaries();
                    } else {
                        executionStream = executionHistory.findAllSummaries(criteria);
                    }
                }
            }

            Integer offset = intParam(request, OFFSET_PARAM);
            if (offset != null && offset > 0) {
                executionStream = executionStream.skip(offset);
            }
            Integer limit = intParam(request, LIMIT_PARAM);
            if (limit != null && limit > 0) {
                executionStream = executionStream.limit(limit);
            }
            List<?> executions = executionStream.collect(Collectors.toList());

            ExecutionOutput output = new ExecutionOutput(executions);
            respondJson(response, ok("Executions listed successfully", output));
        } catch (Exception e) {
            LOG.error("Executions(s) cannot be read!", e);

            respondJson(
                    response,
                    error(String.format("Executions(s) cannot be read! %s", e.getMessage())
                            .trim()));
        }
    }
}
