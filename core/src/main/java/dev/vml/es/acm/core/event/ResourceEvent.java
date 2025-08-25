package dev.vml.es.acm.core.event;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.repo.RepoUtils;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.*;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;

public class ResourceEvent implements Event {

    public static final String ROOT = AcmConstants.VAR_ROOT + "/event";

    public static final String TRIGGERED_AT_PROP = "triggeredAt";

    private final String name;

    private final Calendar triggeredAt;

    private final Map<String, Object> properties;

    public ResourceEvent(Resource resource) {
        this.name = StringUtils.substringAfter(resource.getPath(), ROOT + "/");
        this.triggeredAt = resource.getValueMap().get(TRIGGERED_AT_PROP, Calendar.class);
        this.properties = readProperties(resource);
    }

    private static Map<String, Object> readProperties(Resource resource) {
        Map<String, Object> props = new HashMap<>(resource.getValueMap());
        props.remove(TRIGGERED_AT_PROP);
        return props;
    }

    public static ResourceEvent create(String name, Map<String, Object> props, ResourceResolver resolver) {
        try {
            Resource root = RepoUtils.ensure(resolver, ROOT, JcrResourceConstants.NT_SLING_FOLDER, true);
            Resource result = root.getChild(name);

            Map<String, Object> updatedProps = new HashMap<>();
            updatedProps.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
            updatedProps.putAll(props);
            updatedProps.put(TRIGGERED_AT_PROP, Calendar.getInstance());

            if (result == null) {
                result = resolver.create(root, name, updatedProps);
            } else {
                ModifiableValueMap existingProps = Objects.requireNonNull(result.adaptTo(ModifiableValueMap.class));
                existingProps.putAll(updatedProps);
            }
            resolver.commit();
            return new ResourceEvent(result);
        } catch (PersistenceException e) {
            throw new AcmException(String.format("Cannot create event '%s'!", name));
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Calendar getTriggeredAt() {
        return triggeredAt;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getPath() {
        return ROOT + "/" + name;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("name", name)
                .append("triggeredAt", triggeredAt)
                .toString();
    }
}
