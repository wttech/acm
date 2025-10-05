package dev.vml.es.acm.core.servlet;

import static dev.vml.es.acm.core.util.ServletResult.error;
import static dev.vml.es.acm.core.util.ServletResult.ok;
import static dev.vml.es.acm.core.util.ServletUtils.respondJson;
import static dev.vml.es.acm.core.util.ServletUtils.stringsParam;

import dev.vml.es.acm.core.code.FileManager;
import dev.vml.es.acm.core.servlet.output.FileOutput;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.Servlet;
import javax.servlet.http.Part;
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
            ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + FileServlet.RT,
            ServletResolverConstants.SLING_SERVLET_METHODS + "=POST",
            ServletResolverConstants.SLING_SERVLET_METHODS + "=DELETE",
            ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
        })
public class FileServlet extends SlingAllMethodsServlet {

    public static final String RT = "acm/api/file";

    private static final Logger LOG = LoggerFactory.getLogger(FileServlet.class);

    private static final String PATH_PARAM = "path";

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            List<String> filesUploaded = new LinkedList<>();
            FileManager manager = new FileManager(request.getResourceResolver());
            for (Part part : request.getParts()) {
                if (part.getSubmittedFileName() != null) {
                    String filePath = manager.save(part.getSubmittedFileName(), part.getInputStream());
                    filesUploaded.add(filePath);
                }
            }
            FileOutput output = new FileOutput(filesUploaded);
            respondJson(response, ok("Files uploaded successfully", output));
        } catch (Exception e) {
            LOG.error("Files cannot be uploaded!", e);
            respondJson(
                    response,
                    error(String.format(
                            "Files cannot be uploaded! %s", e.getMessage().trim())));
        }
    }

    @Override
    protected void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            List<String> paths = stringsParam(request, PATH_PARAM);
            FileManager manager = new FileManager(request.getResourceResolver());
            List<String> deleted = manager.deleteAll(paths);
            FileOutput output = new FileOutput(deleted);
            respondJson(response, ok("Files deleted successfully", output));
        } catch (Exception e) {
            LOG.error("Files cannot be deleted!", e);
            respondJson(
                    response,
                    error(String.format(
                            "Files cannot be deleted! %s", e.getMessage().trim())));
        }
    }
}
