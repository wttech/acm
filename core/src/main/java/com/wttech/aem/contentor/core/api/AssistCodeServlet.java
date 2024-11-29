package com.wttech.aem.contentor.core.api;

import com.wttech.aem.contentor.core.assist.Assistance;
import com.wttech.aem.contentor.core.assist.Assistancer;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;

import static com.wttech.aem.contentor.core.util.ServletResult.*;
import static com.wttech.aem.contentor.core.util.ServletUtils.respondJson;
import static com.wttech.aem.contentor.core.util.ServletUtils.stringParam;

@Component(
        immediate = true,
        service = Servlet.class,
        property = {
                ServletResolverConstants.SLING_SERVLET_METHODS + "=GET",
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
                ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + AssistCodeServlet.RT
        })
public class AssistCodeServlet extends SlingAllMethodsServlet {

    public static final String RT = "contentor/api/assist-code";

    private static final Logger LOG = LoggerFactory.getLogger(AssistCodeServlet.class);

    public static final String WORD_PARAM = "word";

    @Reference
    private Assistancer assistancer;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String word = stringParam(request, WORD_PARAM);
        if (word == null) {
            respondJson(response, badRequest("Code assistance word is not specified!"));
            return;
        }

        try {
            Assistance assistance = assistancer.forWord(word);
            respondJson(response, ok("Code assistance generated successfully", assistance));
        } catch (Exception e) {
            LOG.error("Cannot generate code assistance", e);
            respondJson(response, error(String.format("Code assistance cannot be generated. Error: %s", e.getMessage())));
        }
    }
}
