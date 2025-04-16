package com.vml.es.aem.acm.core.servlet;

import static com.vml.es.aem.acm.core.util.ServletResult.error;
import static com.vml.es.aem.acm.core.util.ServletResult.ok;
import static com.vml.es.aem.acm.core.util.ServletUtils.respondJson;
import static com.vml.es.aem.acm.core.util.ServletUtils.stringsParam;

import com.vml.es.aem.acm.core.snippet.Snippet;
import com.vml.es.aem.acm.core.snippet.SnippetRepository;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.Servlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = {Servlet.class},
        property = {
            ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + SnippetServlet.RT,
            ServletResolverConstants.SLING_SERVLET_METHODS + "=GET",
            ServletResolverConstants.SLING_SERVLET_METHODS + "=POST",
            ServletResolverConstants.SLING_SERVLET_METHODS + "=DELETE",
            ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
        })
public class SnippetServlet extends SlingAllMethodsServlet {

    public static final String RT = "acm/api/snippet";

    private static final Logger LOG = LoggerFactory.getLogger(SnippetServlet.class);

    private static final String PATH_PARAM = "path";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            SnippetRepository repository = new SnippetRepository(request.getResourceResolver());
            List<Snippet> snippets;

            List<String> paths = stringsParam(request, PATH_PARAM);
            if (paths != null) {
                snippets = repository.readAll(paths).sorted().collect(Collectors.toList());
            } else {
                snippets = repository.findAll().sorted().collect(Collectors.toList());
            }
            SnippetOutput output = new SnippetOutput(snippets);
            respondJson(response, ok("Snippets listed successfully", output));
        } catch (Exception e) {
            LOG.error("Snippet(s) cannot be read!", e);

            respondJson(
                    response,
                    error(String.format("Snippet(s) cannot be read! %s", e.getMessage())
                            .trim()));
        }
    }
}
