package com.wttech.aem.contentor.core.servlet;

import static com.wttech.aem.contentor.core.util.ServletResult.*;
import static com.wttech.aem.contentor.core.util.ServletUtils.respondJson;
import static com.wttech.aem.contentor.core.util.ServletUtils.stringParam;

import com.wttech.aem.contentor.core.code.*;
import com.wttech.aem.contentor.core.util.JsonUtils;
import java.io.IOException;
import javax.servlet.Servlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = {Servlet.class},
    property = {
      ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + QueueCodeServlet.RT,
      ServletResolverConstants.SLING_SERVLET_METHODS + "=GET",
      ServletResolverConstants.SLING_SERVLET_METHODS + "=POST",
      ServletResolverConstants.SLING_SERVLET_METHODS + "=DELETE",
      ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
    })
public class QueueCodeServlet extends SlingAllMethodsServlet {

  public static final String RT = "contentor/api/queue-code";

  public static final String JOB_ID_PARAM = "jobId";

  private static final Logger LOG = LoggerFactory.getLogger(QueueCodeServlet.class);

  @Reference private ExecutionQueue executionQueue;

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws IOException {
    try {
      ExecuteCodeInput input = JsonUtils.readJson(request.getInputStream(), ExecuteCodeInput.class);
      if (input == null) {
        respondJson(response, badRequest("Code input is not specified!"));
        return;
      }

      Code code = input.getCode();
      ExecutionContext context = executionQueue.createContext(code, request.getResourceResolver());

      ExecutionMode mode = ExecutionMode.of(input.getMode()).orElse(null);
      if (mode == null) {
        respondJson(
            response, badRequest(String.format("Execution mode '%s' is not supported!", mode)));
        return;
      }
      context.setMode(mode);

      Execution execution = executionQueue.submit(code).orElse(null);
      if (execution == null) {
        respondJson(response, error("Code cannot be queued for execution!"));
        return;
      }

      respondJson(
          response,
          ok(
              String.format("Code from '%s' queued for execution successfully", code.getId()),
              execution));
    } catch (Exception e) {
      LOG.error("Code input cannot be read!", e);
      respondJson(
          response,
          badRequest(String.format("Code input cannot be read! %s", e.getMessage()).trim()));
    }
  }

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws IOException {
    String jobId = stringParam(request, JOB_ID_PARAM);
    if (jobId == null) {
      respondJson(response, badRequest("Job ID is not specified!"));
      return;
    }

    try {
      Execution execution = executionQueue.read(jobId).orElse(null);
      if (execution == null) {
        respondJson(response, notFound(String.format("Job with ID '%s' not found!", jobId)));
        return;
      }

      respondJson(response, ok("Job found successfully", execution));
    } catch (Exception e) {
      LOG.error("Job cannot be read!", e);
      respondJson(response, error(String.format("Job cannot be read! %s", e.getMessage()).trim()));
    }
  }

  @Override
  protected void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws IOException {
    String jobId = stringParam(request, JOB_ID_PARAM);
    if (jobId == null) {
      respondJson(response, badRequest("Job ID is not specified!"));
      return;
    }

    try {
      Execution execution = executionQueue.read(jobId).orElse(null);
      if (execution == null) {
        respondJson(response, notFound(String.format("Job with ID '%s' not found!", jobId)));
        return;
      }

      executionQueue.stop(jobId);

      respondJson(response, ok("Job stopped successfully", execution));
    } catch (Exception e) {
      LOG.error("Job cannot be stopped!", e);
      respondJson(
          response, error(String.format("Job cannot be stopped! %s", e.getMessage()).trim()));
    }
  }
}
