package com.wttech.aem.contentor.core.servlet;

import com.wttech.aem.contentor.core.script.Script;
import com.wttech.aem.contentor.core.script.ScriptRepository;
import com.wttech.aem.contentor.core.script.ScriptType;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.wttech.aem.contentor.core.util.ServletResult.error;
import static com.wttech.aem.contentor.core.util.ServletResult.ok;
import static com.wttech.aem.contentor.core.util.ServletUtils.*;

@Component(
        service = {Servlet.class},
        property = {
                ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + ScriptServlet.RT,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=GET",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=POST",
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
        })
public class ScriptServlet extends SlingAllMethodsServlet {

    public static final String RT = "contentor/api/script";

    private static final Logger LOG = LoggerFactory.getLogger(ScriptServlet.class);

    private static final String PATH_PARAM = "path";

    private static final String TYPE_PARAM = "type";

    private static final String ACTION_PARAM = "action";

    private enum Action {
        ENABLE, DISABLE;

        public static Optional<Action> of(String name) {
            return Arrays.stream(Action.values())
                    .filter(a -> a.name().equalsIgnoreCase(name))
                    .findFirst();
        }
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        ScriptType type = ScriptType.of(stringParam(request, TYPE_PARAM)).orElse(null);
        if (type == null) {
            respondJson(response, error("Script type parameter is not specified!"));
            return;
        }

        try {
            ScriptRepository repository = new ScriptRepository(request.getResourceResolver());
            List<Script> scripts;

            List<String> paths = stringsParam(request, PATH_PARAM);
            if (!paths.isEmpty()) {
                scripts = repository.readAll(paths).sorted().collect(Collectors.toList());
            } else {
                scripts = repository.findAll(type).sorted().collect(Collectors.toList());
            }
            ScriptOutput output = new ScriptOutput(scripts);
            respondJson(response, ok("Scripts listed successfully", output));
        } catch (Exception e) {
            LOG.error("Scripts cannot be read!", e);
            respondJson(response, error(String.format("Scripts cannot be read! %s", e.getMessage()).trim()));
        }
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String path = stringParam(request, PATH_PARAM);
        if (path == null || path.isEmpty()) {
            respondJson(response, error("Script path parameter is not specified!"));
            return;
        }

        Optional<Action> action = Action.of(stringParam(request, ACTION_PARAM));
        if (!action.isPresent()) {
            respondJson(response, error("Invalid action parameter! Must be either 'enable' or 'disable'"));
            return;
        }

        try {
            ScriptRepository repository = new ScriptRepository(request.getResourceResolver());
            
            switch (action.get()) {
                case ENABLE:
                    repository.enable(path);
                    respondJson(response, ok("Script enabled successfully"));
                    break;
                case DISABLE:
                    repository.disable(path);
                    respondJson(response, ok("Script disabled successfully"));
                    break;
            }
        } catch (Exception e) {
            LOG.error("Cannot perform script action", e);
            respondJson(response, error(e.getMessage()));
        }
    }
}
