package dev.vml.es.acm.core.notification;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NotifierFactory<N extends Notifier<?>> {

    public static final String ID_DEFAULT = "default";

    private static final Logger LOG = LoggerFactory.getLogger(NotifierFactory.class);

    private static final String PID_DEFAULT = "default";

    private String configPid;

    private N notifier;

    protected void create(Map<String, Object> props, Supplier<N> supplier) {
        this.configPid = getConfigPid(props);
        try {
            this.notifier = supplier.get();
        } catch (Exception e) {
            LOG.error("Cannot create notifier for PID '{}'!", configPid, e);
        }
    }

    protected void destroy(Map<String, Object> props) {
        if (this.notifier != null) {
            try {
                this.notifier.close();
            } catch (IOException e) {
                LOG.error("Cannot clean up notifier for PID '{}'!", configPid, e);
            }
            this.notifier = null;
            this.configPid = null;
        }
    }

    private String getConfigPid(Map<String, Object> props) {
        String pid = (String) props.getOrDefault(Constants.SERVICE_PID, PID_DEFAULT);
        return StringUtils.substringAfter(pid, "~");
    }

    public N getNotifier() {
        return Optional.ofNullable(notifier).orElseThrow(() -> new NotificationException("Notifier not created properly!"));
    }
}
