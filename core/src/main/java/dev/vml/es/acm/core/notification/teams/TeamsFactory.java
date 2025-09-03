package dev.vml.es.acm.core.notification.teams;

import dev.vml.es.acm.core.notification.NotifierFactory;

import java.util.Map;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(service = TeamsFactory.class, immediate = true)
@Designate(ocd = TeamsFactory.Config.class, factory = true)
public class TeamsFactory extends NotifierFactory<Teams> {

    @Activate
    @Modified
    public void factory(Map<String, Object> props, Config config) {
        create(props, () -> new Teams(config.id(), config.webhookUrl(), config.enabled(), config.timeoutMillis()));
    }

    @Deactivate
    public void destroy(Map<String, Object> props) {
        destroy(props);
    }

    @ObjectClassDefinition(name = "AEM Content Manager - Teams Factory")
    public @interface Config {

        @AttributeDefinition(name = "ID", description = "Unique identifier for this configuration")
        String id() default NotifierFactory.ID_DEFAULT;

        @AttributeDefinition(name = "Webhook URL", type = AttributeType.PASSWORD, description = "Determines target Teams channel and authentication")
        String webhookUrl();

        @AttributeDefinition(name = "Enabled")
        boolean enabled() default true;

        @AttributeDefinition(name = "Timeout Millis", description = "HTTP connection and read timeout in milliseconds")
        int timeoutMillis() default 5000;
    }
}
