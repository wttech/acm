package dev.vml.es.acm.core.gui;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vml.es.acm.core.AcmConstants;

@Model(adaptables = SlingHttpServletRequest.class)
public class Spa {

    private static final Logger LOG = LoggerFactory.getLogger(Spa.class);

    private static final String ASSETS_ROOT = AcmConstants.APPS_ROOT + "/gui/spa/build/assets";

    @Self
    private SlingHttpServletRequest request;

    public String getCssPath() {
        return findChildResource("index-.*\\.css").orElse(null);
    }

    public String getJsPath() {
        return findChildResource("index-.*\\.js").orElse(null);
    }

    private Optional<String> findChildResource(String regex) {
        Resource resource = request.getResourceResolver().getResource(ASSETS_ROOT);
        if (resource != null) {
            Pattern pattern = Pattern.compile(regex);
            for (Resource child : resource.getChildren()) {
                Matcher matcher = pattern.matcher(child.getName());
                if (matcher.matches()) {
                    return Optional.of(child.getPath());
                }
            }
        } else {
            LOG.error("GUI child resource of path '{}' not found by regexp '{}'", ASSETS_ROOT, regex);
        }
        return Optional.empty();
    }
}
