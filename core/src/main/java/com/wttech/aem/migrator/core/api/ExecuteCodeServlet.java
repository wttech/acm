package com.wttech.aem.migrator.core.api;

import static com.wttech.aem.migrator.core.util.ServletUtils.respondJson;
import static javax.servlet.http.HttpServletResponse.*;

import com.wttech.aem.migrator.core.script.Executor;
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
        respondJson(response, new Result(SC_OK, "Code executed successfully"));
    }
}
