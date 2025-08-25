package dev.vml.es.acm.core.repo;

import dev.vml.es.acm.core.util.TypeValueMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ValueMap;

public class RepoValueMap extends TypeValueMap {

    private final RepoResource resource;

    public RepoValueMap(RepoResource resource, ValueMap base) {
        super(base);
        this.resource = resource;
    }

    public Map<String, String> stringify() {
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : keySet()) {
            if (JcrConstants.JCR_DATA.equals(key)) {
                result.put(key, toStringJcrData());
            } else {
                result.put(key, toStringDefault(key));
            }
        }
        return result;
    }

    private String toStringDefault(String key) {
        return StringUtils.abbreviate(get(key, String.class), ABBREVIATE_LENGTH);
    }

    private String toStringJcrData() {
        long size = resource.isNode() ? resource.propertyLength(JcrConstants.JCR_DATA) : 0;
        return String.format("{size=%s}", FileUtils.byteCountToDisplaySize(size));
    }
}
