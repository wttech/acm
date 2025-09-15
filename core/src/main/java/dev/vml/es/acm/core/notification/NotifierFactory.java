package dev.vml.es.acm.core.notification;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NotifierFactory<N extends Notifier<? extends Serializable>> {

    public static final String ID_DEFAULT = "default";

    private static final Logger LOG = LoggerFactory.getLogger(NotifierFactory.class);

    private String configId;

    private N notifier;

    protected void create(Map<String, Object> props, Supplier<N> supplier) {
        this.configId = getConfigId(props);
        try {
            this.notifier = supplier.get();
        } catch (Exception e) {
            LOG.error("Cannot create notifier for ID '{}'!", configId, e);
        }
    }

    protected void destroy(Map<String, Object> props) {
        if (this.notifier != null) {
            try {
                this.notifier.close();
            } catch (IOException e) {
                LOG.error("Cannot clean up notifier for ID '{}'!", configId, e);
            }
            this.notifier = null;
            this.configId = null;
        }
    }

    private String getConfigId(Map<String, Object> props) {
        return StringUtils.defaultIfBlank((String) props.get("id"), ID_DEFAULT);
    }

    public N getNotifier() {
        return Optional.ofNullable(notifier)
                .orElseThrow(() -> new NotificationException("Notifier not created properly!"));
    }
}
