package com.vml.es.aem.acm.core.mock;

import java.io.IOException;
import java.util.Iterator;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vml.es.aem.acm.core.code.Executor;
import com.vml.es.aem.acm.core.util.ResourceUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
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
        },
        configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = MockFilter.Config.class)
public class MockFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(MockFilter.class);

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private MockManager manager;

    private Config config;

    @ObjectClassDefinition(name = "AEM Content Manager - Mock HTTP Filter")
    public @interface Config {

        @AttributeDefinition(name = "Enabled")
        boolean enabled() default true;

        @AttributeDefinition(name = "Whiteboard Filter Regex")
        String[] osgi_http_whiteboard_filter_regex() default { "/mock/.*" };
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
            MockRepository repository = new MockRepository(manager, resolver);
            try {
                Iterator<Resource> it = repository.findStubs().iterator();
                while (it.hasNext()) {
                    Resource mock = it.next();
                    if (!manager.isSpecial(mock)) {
                        // TODO connect somehow MockScript with executor.createContext(mock.getPath(), ExecutionMode.RUN, mock, resolver);
                        if (mock.request(request)) {
                            mock.respond(request, response);
                        }
                        return;
                    }
                }
            } catch (MockException e) {
                LOG.error("Stubs error!", e);

                Mock mock = repository.findSpecialStub(MockManager.FAIL_PATH).orElse(null);
                if (mock != null) {
                    try {
                        mock.fail(request, response, e);
                    } catch (MockException e2) {
                        LOG.error("Stubs fail error!", e2);
                        response.sendError(
                                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Stubs fail error. " + e.getMessage());
                    }
                } else {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Stubs error. " + e.getMessage());
                }
                return;
            }

            Mock mock = repository.findSpecialStub(MockManager.MISSING_PATH).orElse(null);
            if (mock != null) {
                try {
                    mock.respond(request, response);
                } catch (MockException e) {
                    LOG.error("Stubs missing error!", e);
                    response.sendError(
                            HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Stubs missing error. " + e.getMessage());
                }
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Mock not found!");
            }
            return;
        } catch (LoginException e) {
            LOG.error("Stubs repository error!", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Stubs error. " + e.getMessage());
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