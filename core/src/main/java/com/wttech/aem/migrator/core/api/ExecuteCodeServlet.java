package com.wttech.aem.migrator.core.api;

import static com.wttech.aem.migrator.core.util.ServletUtils.respondJson;
import static javax.servlet.http.HttpServletResponse.*;

import com.wttech.aem.migrator.core.script.Executor;
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
        immediate = true,
        service = Servlet.class,
        property = {
            ServletResolverConstants.SLING_SERVLET_METHODS + "=POST",
            ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
            ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + ExecuteCodeServlet.RT
        })
public class ExecuteCodeServlet extends SlingAllMethodsServlet {

    public static final String RT = "migrator/api/execute-code";

    private static final Logger LOG = LoggerFactory.getLogger(ExecuteCodeServlet.class);

    @Reference
    private Executor executor;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            var input = JsonUtils.readJson(request.getInputStream(), ExecuteCodeInput.class);
            if (input == null) {
                respondJson(response, new Result(SC_BAD_REQUEST, "Code input is not specified!"));
                return;
            }

            var code = input.getCode();

            try {
                var execution = executor.execute(code);

                // TODO should this servlet also save execution in history?
                // history.save(execution)

                respondJson(
                        response,
                        new Result(
                                SC_OK, String.format("Code from '%s' executed successfully", code.getId()), execution));
            } catch (Exception e) {
                LOG.error("Code from '{}' cannot be executed!", code.getId(), e);
                respondJson(
                        response,
                        new Result(
                                SC_INTERNAL_SERVER_ERROR,
                                String.format(
                                        "Code from '%s' cannot be executed. Error: %s", code.getId(), e.getMessage())));
            }
            respondJson(response, new Result(SC_OK, "Code executed successfully"));
        } catch (Exception e) {
            LOG.error("Code input cannot be read!", e);
            respondJson(response, new Result(SC_BAD_REQUEST, "Cannot read code input!"));
            return;
        }
    }
}
