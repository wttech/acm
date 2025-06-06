package dev.vml.es.acm.core.servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static dev.vml.es.acm.core.util.ServletResult.error;
import static dev.vml.es.acm.core.util.ServletResult.ok;
import static dev.vml.es.acm.core.util.ServletUtils.respondJson;

@Component(
        service = {Servlet.class},
        property = {
            ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + FileServlet.RT,
            ServletResolverConstants.SLING_SERVLET_METHODS + "=POST",
            ServletResolverConstants.SLING_SERVLET_METHODS + "=DELETE",
            ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
        })
public class FileServlet extends SlingAllMethodsServlet {

    public static final String RT = "acm/api/file";

    private static final Logger LOG = LoggerFactory.getLogger(FileServlet.class);

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            // TODO support uploading multiple files
            File file = new File("/tmp/acm/file/yyyy/mm/dd/number/originalFileName.txt");
            FileOutput output = new FileOutput(Collections.singletonList(file));
            respondJson(response, ok("File uploaded successfully", output));
        } catch (Exception e) {
            LOG.error("File cannot be uploaded!", e);
            respondJson(response, error(String.format("File cannot be uploaded! %s", e.getMessage().trim())));
        }
    }

    @Override
    protected void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            // TODO support deleting multiple files
            File file = new File("/tmp/acm/file/yyyy/mm/dd/number/originalFileName.txt");
            FileOutput output = new FileOutput(Collections.singletonList(file));
            respondJson(response, ok("File deleted successfully", output));
        } catch (Exception e) {
            LOG.error("File cannot be deleted!", e);
            respondJson(response, error(String.format("File cannot be deleted! %s", e.getMessage().trim())));
        }
    }

}
