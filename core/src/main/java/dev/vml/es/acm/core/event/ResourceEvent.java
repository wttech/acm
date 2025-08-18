package dev.vml.es.acm.core.event;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.repo.RepoResource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ResourceEvent implements Event {

    public static final String ROOT = AcmConstants.VAR_ROOT + "/event";

    public static final String TRIGGERED_AT_PROP = "triggeredAt";

    private final String name;

    private final Calendar triggeredAt;

    public ResourceEvent(Resource resource) {
        this.name = StringUtils.substringAfter(resource.getPath(), ROOT + "/");
        this.triggeredAt = resource.getValueMap().get(TRIGGERED_AT_PROP, Calendar.class);
    }

    public static ResourceEvent create(String name, ResourceResolver resolver) {
        RepoResource result = RepoResource.of(resolver, String.format("%s/%s", ROOT, name));
        Map<String, Object> properties = new HashMap<>();
        properties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        properties.put(TRIGGERED_AT_PROP, Calendar.getInstance());
        result.save(properties);
        return new ResourceEvent(result.require());
    }

    public String getName() {
        return name;
    }

    public Calendar getTriggeredAt() {
        return triggeredAt;
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
