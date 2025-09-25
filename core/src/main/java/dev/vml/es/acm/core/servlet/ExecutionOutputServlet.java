package dev.vml.es.acm.core.servlet;

import static dev.vml.es.acm.core.util.ServletResult.*;
import static dev.vml.es.acm.core.util.ServletUtils.*;

import dev.vml.es.acm.core.code.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.Servlet;
import org.apache.commons.io.IOUtils;
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
            ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + ExecutionOutputServlet.RT
        })
public class ExecutionOutputServlet extends SlingAllMethodsServlet {

    public static final String RT = "acm/api/execution-output";

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionOutputServlet.class);

    private static final String EXECUTION_ID_PARAM = "executionId";

    private static final String NAME_PARAM = "name";

    @Reference
    private transient ExecutionQueue queue;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String id = stringParam(request, EXECUTION_ID_PARAM);
        if (id == null) {
            respondJson(response, badRequest(String.format("Execution ID is not specified!", EXECUTION_ID_PARAM)));
            return;
        }
        String name = stringParam(request, NAME_PARAM);
        if (name == null) {
            respondJson(response, badRequest(String.format("Execution output name is not specified!", NAME_PARAM)));
            return;
        }

        try {
            ExecutionHistory executionHistory = new ExecutionHistory(request.getResourceResolver());
            Execution execution = executionHistory.findById(id).orElse(null);
            if (execution == null) {
                respondJson(response, notFound(String.format("Execution with id '%s' not found!", id)));
                return;
            }

            ExecutionOutput.Name outputName = ExecutionOutput.Name.byId(name).orElse(null);
            if (outputName != null) {
                // Predefined outputs
                switch (outputName) {
                    case ARCHIVE:
                        respondArchive(response, execution, executionHistory);
                        break;
                    case CONSOLE:
                        respondConsole(response, execution);
                        break;
                }
            } else {
                // Dynamic output
                Output output = execution.getOutputs().stream()
                        .filter(o -> o.getName().equals(name))
                        .findFirst()
                        .orElse(null);
                if (output == null) {
                    respondJson(
                            response,
                            notFound(String.format("Execution output '%s' not found in execution '%s'!", name, id)));
                    return;
                }
                respondOutput(response, name, executionHistory, execution, output);
            }
        } catch (Exception e) {
            LOG.error("Execution output '{}' cannot be read for execution '{}'", name, id, e);
            respondJson(
                    response,
                    error(String.format(
                                    "Execution output '%s' cannot be read for execution '%s'! Error: %s",
                                    name, id, e.getMessage())
                            .trim()));
        }
    }

    private void respondConsole(SlingHttpServletResponse response, Execution execution) throws IOException {
        respondDownload(response, "text/plain", String.format("execution-%s.console.log", execution.getId()));
        InputStream consoleStream =
                new ByteArrayInputStream(execution.getOutput().getBytes());
        IOUtils.copy(consoleStream, response.getOutputStream());
    }

    private void respondArchive(
            SlingHttpServletResponse response, Execution execution, ExecutionHistory executionHistory)
            throws IOException {
        respondDownload(response, "application/zip", String.format("execution-%s.outputs.zip", execution.getId()));
        try (ZipOutputStream zipStream = new ZipOutputStream(response.getOutputStream())) {
            // Console log
            ZipEntry consoleEntry = new ZipEntry("console.log");
            zipStream.putNextEntry(consoleEntry);
            zipStream.write(execution.getOutput().getBytes());
            zipStream.closeEntry();

            // Dynamic outputs
            for (Output output : execution.getOutputs()) {
                ZipEntry outputEntry = new ZipEntry(output.getDownloadName());
                zipStream.putNextEntry(outputEntry);
                try (InputStream outputStream = executionHistory.readOutputByName(execution, output.getName())) {
                    IOUtils.copy(outputStream, zipStream);
                }
                zipStream.closeEntry();
            }
        }
    }

    private void respondOutput(
            SlingHttpServletResponse response,
            String name,
            ExecutionHistory executionHistory,
            Execution execution,
            Output output)
            throws IOException {
        respondDownload(response, output.getMimeType(), output.getDownloadName());
        InputStream inputStream = executionHistory.readOutputByName(execution, name);
        IOUtils.copy(inputStream, response.getOutputStream());
    }

    private void respondDownload(SlingHttpServletResponse response, String mimeType, String fileName) {
        response.setContentType(mimeType);
        response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", fileName));
    }
}
