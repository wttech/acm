package dev.vml.es.acm.core.servlet;

import dev.vml.es.acm.core.code.FileManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static dev.vml.es.acm.core.util.ServletResult.error;
import static dev.vml.es.acm.core.util.ServletResult.ok;
import static dev.vml.es.acm.core.util.ServletUtils.respondJson;
import static dev.vml.es.acm.core.util.ServletUtils.stringsParam;

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

    @Reference
    private FileManager manager;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            List<File> filesUploaded = new LinkedList<>();
            for (Part part : request.getParts()) {
                if (part.getSubmittedFileName() != null) {
                    File file = manager.save(part.getInputStream(), part.getSubmittedFileName());
                    filesUploaded.add(file);
                }
            }
            FileOutput output = new FileOutput(filesUploaded);
            respondJson(response, ok("Files uploaded successfully", output));
        } catch (Exception e) {
            LOG.error("Files cannot be uploaded!", e);
            respondJson(response, error(String.format("Files cannot be uploaded! %s", e.getMessage().trim())));
        }
    }

    @Override
    protected void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            List<String> paths = stringsParam(request, PATH_PARAM);
            List<File> deleted = manager.deleteAll(paths);
            FileOutput output = new FileOutput(deleted);
            respondJson(response, ok("Files deleted successfully", output));
        } catch (Exception e) {
            LOG.error("Files cannot be deleted!", e);
            respondJson(response, error(String.format("Files cannot be deleted! %s", e.getMessage().trim())));
        }
    }

}
