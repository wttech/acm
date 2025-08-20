package dev.vml.es.acm.core.servlet;

import static dev.vml.es.acm.core.util.ServletResult.*;
import static dev.vml.es.acm.core.util.ServletUtils.*;

import dev.vml.es.acm.core.code.Code;
import dev.vml.es.acm.core.gui.SpaSettings;
import dev.vml.es.acm.core.script.Script;
import dev.vml.es.acm.core.script.ScriptRepository;
import dev.vml.es.acm.core.script.ScriptStats;
import dev.vml.es.acm.core.script.ScriptType;
import dev.vml.es.acm.core.util.JsonUtils;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
            ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + ScriptServlet.RT,
            ServletResolverConstants.SLING_SERVLET_METHODS + "=GET",
            ServletResolverConstants.SLING_SERVLET_METHODS + "=POST",
            ServletResolverConstants.SLING_SERVLET_METHODS + "=DELETE",
            ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
        })
public class ScriptServlet extends SlingAllMethodsServlet {

    public static final String RT = "acm/api/script";

    private static final Logger LOG = LoggerFactory.getLogger(ScriptServlet.class);

    private static final String ID_PARAM = "id";

    private static final String TYPE_PARAM = "type";

    private static final String STATS_LIMIT_PARAM = "statsLimit";

    @Reference
    private SpaSettings spaSettings;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        long statsLimit =
                Optional.ofNullable(longParam(request, STATS_LIMIT_PARAM)).orElse(spaSettings.getScriptStatsLimit());
        if (statsLimit < 0) {
            respondJson(
                    response,
                    error(String.format("Script stats limit '%d' cannot be a negative integer!", statsLimit)));
            return;
        }

        try {
            ScriptRepository repository = new ScriptRepository(request.getResourceResolver());
            List<Script> scripts;

            List<String> ids = stringsParam(request, ID_PARAM);
            if (ids != null) {
                scripts = repository.readAll(ids).sorted().collect(Collectors.toList());
            } else {
                String typeValue = stringParam(request, TYPE_PARAM);
                ScriptType type = ScriptType.of(typeValue).orElse(null);
                if (type == null) {
                    respondJson(response, error(String.format("Script type '%s' is not supported!", typeValue)));
                    return;
                }
                scripts = repository.findAll(type).sorted().collect(Collectors.toList());
            }
            List<ScriptStats> stats = scripts.stream()
                    .map(Script::getPath)
                    .map(path -> ScriptStats.forCompletedByPath(request.getResourceResolver(), path, statsLimit))
                    .collect(Collectors.toList());

            ScriptListOutput output = new ScriptListOutput(scripts, stats);
            respondJson(response, ok("Scripts listed successfully", output));
        } catch (Exception e) {
            LOG.error("Scripts cannot be read!", e);
            respondJson(
                    response,
                    error(String.format("Scripts cannot be read! %s", e.getMessage())
                            .trim()));
        }
    }

    // TODO introduce 'action' param to handle 'sync all' outside of regular post used for saving scripts
    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            ScriptInput input = JsonUtils.read(request.getInputStream(), ScriptInput.class);
            if (input == null) {
                respondJson(response, badRequest("Script input is not specified!"));
                return;
            }

            Code code = input.getCode();

            ScriptRepository repository = new ScriptRepository(request.getResourceResolver());
            Script script = repository.save(code);

            ScriptOutput output = new ScriptOutput(script);
            respondJson(response, ok("Script saved successfully", output));
        } catch (Exception e) {
            LOG.error("Script cannot be saved!", e);
            respondJson(response, error("Script cannot be saved! " + e.getMessage()));
        }
    }

    @Override
    protected void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        // TODO implement this
        respondJson(response, error("Script deletion is not implemented yet!"));
    }
}
