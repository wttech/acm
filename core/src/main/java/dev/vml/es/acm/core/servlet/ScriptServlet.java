package dev.vml.es.acm.core.servlet;

import static dev.vml.es.acm.core.util.ServletResult.error;
import static dev.vml.es.acm.core.util.ServletResult.ok;
import static dev.vml.es.acm.core.util.ServletUtils.*;

import com.day.cq.replication.Replicator;
import dev.vml.es.acm.core.gui.SpaSettings;
import dev.vml.es.acm.core.replication.Activator;
import dev.vml.es.acm.core.script.Script;
import dev.vml.es.acm.core.script.ScriptRepository;
import dev.vml.es.acm.core.script.ScriptStats;
import dev.vml.es.acm.core.script.ScriptType;
import java.io.IOException;
import java.util.Arrays;
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
            ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
        })
public class ScriptServlet extends SlingAllMethodsServlet {

    public static final String RT = "acm/api/script";

    private static final Logger LOG = LoggerFactory.getLogger(ScriptServlet.class);

    private static final String ID_PARAM = "id";

    private static final String TYPE_PARAM = "type";

    private static final String ACTION_PARAM = "action";

    private static final String STATS_LIMIT_PARAM = "statsLimit";

    private enum Action {
        ENABLE,
        DISABLE,
        SYNC_ALL;

        public static Optional<Action> of(String name) {
            return Arrays.stream(Action.values())
                    .filter(a -> a.name().equalsIgnoreCase(name))
                    .findFirst();
        }
    }

    @Reference
    private Replicator replicator;

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
                ScriptType type =
                        ScriptType.of(stringParam(request, TYPE_PARAM)).orElse(null);
                if (type == null) {
                    respondJson(response, error("Script type parameter is not specified!"));
                    return;
                }
                scripts = repository.findAll(type).sorted().collect(Collectors.toList());
            }
            List<ScriptStats> stats = scripts.stream()
                    .map(Script::getPath)
                    .map(path -> ScriptStats.forCompletedByPath(request.getResourceResolver(), path, statsLimit))
                    .collect(Collectors.toList());

            ScriptOutput output = new ScriptOutput(scripts, stats);
            respondJson(response, ok("Scripts listed successfully", output));
        } catch (Exception e) {
            LOG.error("Scripts cannot be read!", e);
            respondJson(
                    response,
                    error(String.format("Scripts cannot be read! %s", e.getMessage())
                            .trim()));
        }
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        Optional<Action> action = Action.of(stringParam(request, ACTION_PARAM));
        if (!action.isPresent()) {
            respondJson(response, error("Invalid action parameter! Must be either 'enable', 'disable' or 'sync_all'"));
            return;
        }

        List<String> paths = stringsParam(request, ID_PARAM);
        if ((action.get() == Action.ENABLE || action.get() == Action.DISABLE) && paths == null) {
            respondJson(response, error("Script path parameter is not specified!"));
            return;
        }

        try {
            ScriptRepository repository = new ScriptRepository(request.getResourceResolver());
            Activator activator = new Activator(request.getResourceResolver(), replicator);

            switch (action.get()) {
                case ENABLE:
                    paths.forEach(repository::enable);
                    respondJson(response, ok(String.format("%d script(s) enabled successfully", paths.size())));
                    break;
                case DISABLE:
                    paths.forEach(repository::disable);
                    respondJson(response, ok(String.format("%d script(s) disabled successfully", paths.size())));
                    break;
                case SYNC_ALL:
                    repository.clean();
                    activator.reactivateTree(ScriptRepository.ROOT);
                    respondJson(response, ok("Scripts synchronized successfully"));
                    break;
            }
        } catch (Exception e) {
            LOG.error("Cannot perform script action", e);
            respondJson(response, error(e.getMessage()));
        }
    }
}
