package dev.vml.es.acm.core.mock;

import dev.vml.es.acm.core.code.CodeContext;
import dev.vml.es.acm.core.code.script.MockScript;
import dev.vml.es.acm.core.code.script.MockScriptType;
import dev.vml.es.acm.core.osgi.OsgiContext;
import dev.vml.es.acm.core.util.ResolverUtils;
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
        service = {Filter.class, MockHttpFilter.class},
        property = {
            HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT + "=("
                    + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=*)"
        })
@Designate(ocd = MockHttpFilter.Config.class)
public class MockHttpFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(MockHttpFilter.class);

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private OsgiContext osgiContext;

    private Config config;

    public MockStatus checkStatus() {
        return new MockStatus(config.enabled());
    }

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

        try (ResourceResolver resolver = ResolverUtils.mockResolver(resolverFactory)) {
            CodeContext codeContext = new CodeContext(osgiContext, resolver);
            MockRepository repository = new MockRepository(resolver);

            try {
                Iterator<Mock> it = repository.findAll().iterator();
                while (it.hasNext()) {
                    Mock candidateMock = it.next();
                    if (!repository.isSpecial(candidateMock.getId())) {
                        MockContext mockContext = new MockContext(codeContext, candidateMock);
                        codeContext.prepareMock(mockContext);
                        MockScript mock = new MockScript(mockContext, MockScriptType.REGULAR);
                        if (mock.request(request)) {
                            mock.respond(request, response);
                            return;
                        }
                    }
                }
            } catch (MockException e) {
                LOG.error("Mock error!", e);

                Mock failScript =
                        repository.findSpecial(MockRepository.FAIL_PATH).orElse(null);
                if (failScript != null) {
                    try {
                        MockContext mockContext = new MockContext(codeContext, failScript);
                        codeContext.prepareMock(mockContext);
                        MockScript mock = new MockScript(mockContext, MockScriptType.FAIL);
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

            Mock missingScript =
                    repository.findSpecial(MockRepository.MISSING_PATH).orElse(null);
            if (missingScript != null) {
                try {
                    MockContext mockContext = new MockContext(codeContext, missingScript);
                    codeContext.prepareMock(mockContext);
                    MockScript mock = new MockScript(mockContext, MockScriptType.MISSING);
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

    @ObjectClassDefinition(
            name = "AEM Content Manager - Mock HTTP Filter",
            description =
                    "Dedicated for dynamic response generation for mocking purposes, ideal for simulating 3rd-party system responses when their base URL is redirected to this service.")
    public @interface Config {

        @AttributeDefinition(
                name = "Enabled",
                description =
                        "When disabled, the filter will not process any requests. Also GUI no longer will display the mock scripts.")
        boolean enabled() default false;

        @AttributeDefinition(
                name = "Whiteboard Filter Regex",
                description = "Expressions which define the scope of requests that mock scripts can evaluate.")
        String[] osgi_http_whiteboard_filter_regex() default {"/mock/.*"};
    }
}
