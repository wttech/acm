package com.wttech.aem.acm.core.servlet;

import static com.wttech.aem.acm.core.util.ServletResult.*;
import static com.wttech.aem.acm.core.util.ServletUtils.respondJson;

import com.wttech.aem.acm.core.code.*;
import com.wttech.aem.acm.core.util.JsonUtils;
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
            ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
            ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + DescribeCodeServlet.RT
        })
public class DescribeCodeServlet extends SlingAllMethodsServlet {

    public static final String RT = "acm/api/describe-code";

    private static final Logger LOG = LoggerFactory.getLogger(DescribeCodeServlet.class);

    @Reference
    private Executor executor;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            DescribeCodeInput input = JsonUtils.read(request.getInputStream(), DescribeCodeInput.class);
            if (input == null) {
                respondJson(response, badRequest("Code input is not specified!"));
                return;
            }

            Code code = input.getCode();

            try {
                ExecutionContext context = executor.createContext(code, request.getResourceResolver());
                Description description = executor.describe(context);

                respondJson(
                        response, ok(String.format("Code from '%s' described successfully", code.getId()), description));
            } catch (Exception e) {
                LOG.error("Code from '{}' cannot be described!", code.getId(), e);
                respondJson(
                        response,
                        error(String.format(
                                "Code from '%s' cannot be described. Error: %s", code.getId(), e.getMessage())));
            }
            respondJson(response, ok("Code described successfully"));
        } catch (Exception e) {
            LOG.error("Code input cannot be read!", e);
            respondJson(response, badRequest("Cannot read code input!"));
            return;
        }
    }
}
