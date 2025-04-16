package com.vml.es.aem.acm.core.servlet;

import static com.vml.es.aem.acm.core.util.ServletResult.*;
import static com.vml.es.aem.acm.core.util.ServletUtils.respondJson;
import static com.vml.es.aem.acm.core.util.ServletUtils.stringParam;

import com.vml.es.aem.acm.core.assist.Assistance;
import com.vml.es.aem.acm.core.assist.Assistancer;
import com.vml.es.aem.acm.core.assist.SuggestionType;
import java.io.IOException;
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
            ServletResolverConstants.SLING_SERVLET_METHODS + "=GET",
            ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
            ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + AssistCodeServlet.RT
        })
public class AssistCodeServlet extends SlingAllMethodsServlet {

    public static final String RT = "acm/api/assist-code";

    public static final String WORD_PARAM = "word";

    public static final String TYPE_PARAM = "type";

    private static final Logger LOG = LoggerFactory.getLogger(AssistCodeServlet.class);

    @Reference
    private Assistancer assistancer;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String word = stringParam(request, WORD_PARAM);
        if (word == null) {
            respondJson(response, badRequest("Code assistance word is not specified!"));
            return;
        }
        String typeName = StringUtils.defaultString(stringParam(request, TYPE_PARAM), "all");
        SuggestionType suggestionType = SuggestionType.of(typeName);
        try {
            Assistance assistance = assistancer.forWord(request.getResourceResolver(), suggestionType, word);
            if (suggestionType == SuggestionType.ALL) {
                respondCacheMaxAge(response, assistancer.getCacheMaxAgeAll());
            } else {
                respondCacheMaxAge(response, assistancer.getCacheMaxAgeSpecific());
            }
            respondJson(response, ok("Code assistance generated successfully", assistance));
        } catch (Exception e) {
            LOG.error("Cannot generate code assistance", e);
            respondJson(
                    response, error(String.format("Code assistance cannot be generated. Error: %s", e.getMessage())));
        }
    }

    private void respondCacheMaxAge(SlingHttpServletResponse response, int maxAge) {
        if (maxAge > 0) {
            response.setHeader("Cache-Control", String.format("max-age=%d", maxAge));
        }
    }
}
