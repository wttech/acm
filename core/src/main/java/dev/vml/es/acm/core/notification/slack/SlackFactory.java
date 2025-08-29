package dev.vml.es.acm.core.notification.slack;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(service = SlackFactory.class)
@Designate(ocd = SlackFactory.class, factory = true)
public class SlackFactory {

    public static final String ID_DEFAULT = "default";

    private static final String PID_DEFAULT = "default";

    private final Map<String, Slack> factored = new ConcurrentHashMap<>();

    @Activate
    @Modified
    public void activate(Map<String, Object> props, Config config) {
        factored.put(getConfigPid(props), new Slack(config.id(), config.webhookUrl(), config.enabled()));
    }

    @Deactivate
    public void deactivate(Map<String, Object> props) {
        factored.remove(getConfigPid(props));
    }

    private String getConfigPid(Map<String, Object> props) {
        String pid = (String) props.getOrDefault(Constants.SERVICE_PID, PID_DEFAULT);
        return StringUtils.substringAfter(pid, "~");
    }

    public Collection<Slack> getFactored() {
        return Collections.unmodifiableCollection(factored.values());
    }

    public Slack getById(String id) {
        return factored.values().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new SlackException(
                        String.format("Cannot find Slack with id '%s'! Ensure that it is configured properly.", id)));
    }

    @ObjectClassDefinition(name = "AEM Content Manager - Slack Factory")
    public @interface Config {

        @AttributeDefinition(name = "ID", description = "Unique identifier for this configuration")
        String id() default ID_DEFAULT;

        @AttributeDefinition(name = "Webhook URL", description = "Determines target Slack channel and authentication")
        String webhookUrl();

        @AttributeDefinition(name = "Enabled")
        boolean enabled() default true;
    }
}
