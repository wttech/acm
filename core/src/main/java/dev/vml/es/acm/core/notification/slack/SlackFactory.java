package dev.vml.es.acm.core.notification.slack;

import dev.vml.es.acm.core.notification.NotifierFactory;
import java.util.Map;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(service = SlackFactory.class, immediate = true)
@Designate(ocd = SlackFactory.Config.class, factory = true)
public class SlackFactory extends NotifierFactory<Slack> {

    @Activate
    @Modified
    public void activate(Map<String, Object> props, Config config) {
        addFactored(props, () -> new Slack(config.id(), config.webhookUrl(), config.enabled()));
    }

    @Deactivate
    public void deactivate(Map<String, Object> props) {
        removeFactored(props);
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
