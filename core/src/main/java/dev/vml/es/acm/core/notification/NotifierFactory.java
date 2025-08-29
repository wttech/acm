package dev.vml.es.acm.core.notification;

import dev.vml.es.acm.core.notification.teams.TeamsException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NotifierFactory<N extends Notifier<?>> {

    public static final String ID_DEFAULT = "default";

    private static final Logger LOG = LoggerFactory.getLogger(NotifierFactory.class);

    private static final String PID_DEFAULT = "default";

    private final Map<String, N> factored = new ConcurrentHashMap<>();

    protected void addFactored(Map<String, Object> props, Supplier<N> supplier) {
        String pid = getConfigPid(props);
        try {
            factored.put(pid, supplier.get());
        } catch (Exception e) {
            LOG.error("Cannot create notifier for PID '{}'!", pid, e);
        }
    }

    protected void removeFactored(Map<String, Object> props) {
        String pid = getConfigPid(props);
        N removed = factored.remove(pid);
        if (removed != null) {
            try {
                removed.close();
            } catch (IOException e) {
                LOG.error("Cannot clean up notifier for PID '{}'!", pid, e);
            }
        }
    }

    private String getConfigPid(Map<String, Object> props) {
        String pid = (String) props.getOrDefault(Constants.SERVICE_PID, PID_DEFAULT);
        return StringUtils.substringAfter(pid, "~");
    }

    public Collection<N> getFactored() {
        return Collections.unmodifiableCollection(factored.values());
    }

    public N getById(String id) {
        return factored.values().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new TeamsException(String.format(
                        "Cannot find notifier with id '%s'! Ensure that it is configured properly.", id)));
    }

    public N getDefault() {
        return getById(ID_DEFAULT);
    }
}
