package com.vml.es.aem.acm.core.mock;

import com.vml.es.aem.acm.core.code.ExecutionContext;
import com.vml.es.aem.acm.core.code.ExecutionId;
import com.vml.es.aem.acm.core.code.ExecutionMode;
import com.vml.es.aem.acm.core.code.Executor;
import com.vml.es.aem.acm.core.code.script.MockScript;
import com.vml.es.aem.acm.core.util.ResourceUtils;
import java.io.IOException;
import java.util.Iterator;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.*;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = Filter.class,
        property = {
            HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT + "=("
                    + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=*)"
        })
@Designate(ocd = MockHttpFilter.Config.class)
public class MockHttpFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(MockHttpFilter.class);

    @Reference
    private ResourceResolverFactory resolverFactory;

    private Config config;

    @ObjectClassDefinition(name = "AEM Content Manager - Mock HTTP Filter")
    public @interface Config {

        @AttributeDefinition(name = "Enabled")
        boolean enabled() default true;

        @AttributeDefinition(name = "Whiteboard Filter Regex")
        String[] osgi_http_whiteboard_filter_regex() default {"/mock/.*"};
    }

    @Reference
    private Executor executor;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        if (!config.enabled()) {
            chain.doFilter(req, res);
            return;
        }

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        try (ResourceResolver resolver = ResourceUtils.serviceResolver(resolverFactory, null)) {
            MockScriptRepository repository = new MockScriptRepository(resolver);

            try {
                Iterator<MockScriptExecutable> it = repository.findAll().iterator();
                while (it.hasNext()) {
                    MockScriptExecutable candidateScript = it.next();
                    if (!repository.isSpecial(candidateScript.getId())) {
                        ExecutionContext executionContext = executor.createContext(
                                ExecutionId.generate(), ExecutionMode.RUN, candidateScript, resolver);
                        MockScript mock = new MockScript(executionContext);
                        if (mock.request(request)) {
                            mock.respond(request, response);
                            return;
                        }
                    }
                }
            } catch (MockException e) {
                LOG.error("Mock error!", e);

                MockScriptExecutable failScript =
                        repository.findSpecial(MockScriptRepository.FAIL_PATH).orElse(null);
                if (failScript != null) {
                    try {
                        ExecutionContext executionContext =
                                executor.createContext(ExecutionId.generate(), ExecutionMode.RUN, failScript, resolver);
                        MockScript mock = new MockScript(executionContext);
                        mock.fail(request, response, e);
                    } catch (MockException e2) {
                        LOG.error("Mock fail error!", e2);
                        response.sendError(
                                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Mock fail error. " + e.getMessage());
                    }
                } else {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Mock error. " + e.getMessage());
                }
                return;
            }

            MockScriptExecutable missingScript =
                    repository.findSpecial(MockScriptRepository.MISSING_PATH).orElse(null);
            if (missingScript != null) {
                try {
                    ExecutionContext executionContext =
                            executor.createContext(ExecutionId.generate(), ExecutionMode.RUN, missingScript, resolver);
                    MockScript mock = new MockScript(executionContext);
                    mock.respond(request, response);
                } catch (MockException e) {
                    LOG.error("Mock missing error!", e);
                    response.sendError(
                            HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Mock missing error. " + e.getMessage());
                }
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Mock not found!");
            }
            return;
        } catch (LoginException e) {
            LOG.error("Mock repository error!", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Mock error. " + e.getMessage());
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // do nothing
    }

    @Override
    public void destroy() {
        // do nothing
    }
}
