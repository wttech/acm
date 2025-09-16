package dev.vml.es.acm.core.servlet;

import static dev.vml.es.acm.core.util.ServletResult.*;
import static dev.vml.es.acm.core.util.ServletUtils.respondJson;

import dev.vml.es.acm.core.code.*;
import dev.vml.es.acm.core.util.JsonUtils;
import java.io.IOException;
import java.util.Optional;
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
        immediate = true,
        service = Servlet.class,
        property = {
            ServletResolverConstants.SLING_SERVLET_METHODS + "=POST",
            ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
            ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + DescribeCodeServlet.RT
        })
public class DescribeCodeServlet extends SlingAllMethodsServlet {

    public static final String RT = "acm/api/describe-code";

    private static final Logger LOG = LoggerFactory.getLogger(DescribeCodeServlet.class);

    @Reference
    private transient Executor executor;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            DescribeCodeInput input = JsonUtils.read(request.getInputStream(), DescribeCodeInput.class);
            if (input == null) {
                respondJson(response, badRequest("Code input is not specified!"));
                return;
            }

            Code code = input.getCode();

            try (ExecutionContext context = executor.createContext(
                    ExecutionId.generate(), ExecutionMode.PARSE, code, request.getResourceResolver())) {
                Description description = executor.describe(context);

                respondJson(
                        response,
                        ok(String.format("Code from '%s' described successfully", code.getId()), description));
            } catch (Exception e) {
                LOG.error("Code from '{}' cannot be described!", code.getId(), e);
                String cause = StringUtils.defaultIfBlank(
                        Optional.ofNullable(e.getCause())
                                .map(Throwable::getMessage)
                                .orElse(null),
                        e.getMessage());
                respondJson(
                        response,
                        error(String.format("Code from '%s' cannot be described! Cause: %s", code.getId(), cause)));
            }
        } catch (Exception e) {
            LOG.error("Code input cannot be read!", e);
            respondJson(response, badRequest("Cannot read code input!"));
            return;
        }
    }
}
