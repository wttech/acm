package dev.vml.es.acm.core.snippet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.vml.es.acm.core.AcmException;
import java.io.InputStream;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.api.resource.Resource;

public class Snippet implements Comparable<Snippet> {

    public static final String FILE_EXTENSION = ".yml";

    private final transient Resource resource;

    private final transient SnippetDefinition definition;

    public Snippet(Resource resource) throws AcmException {
        this.resource = resource;
        this.definition = readDefinition();
    }

    public static Optional<Snippet> from(Resource resource) throws AcmException {
        return Optional.ofNullable(resource).filter(Snippet::check).map(Snippet::new);
    }

    public static boolean check(Resource resource) {
        return resource != null
                && resource.isResourceType(JcrConstants.NT_FILE)
                && resource.getName().endsWith(FILE_EXTENSION);
    }

    private SnippetDefinition readDefinition() throws AcmException {
        return Optional.ofNullable(resource.getChild(JcrConstants.JCR_CONTENT))
                .map(r -> SnippetDefinition.fromYaml(r.getPath(), r.adaptTo(InputStream.class)))
                .orElseThrow(() ->
                        new AcmException(String.format("Snippet definition '%s' cannot be read!", resource.getPath())));
    }

    public String getId() {
        return getPath();
    }

    public String getContent() throws AcmException {
        return StringUtils.trim(definition.getContent());
    }

    public String getDocumentation() throws AcmException {
        return StringUtils.trim(definition.getDocumentation());
    }

    @JsonIgnore
    public String getPath() {
        return resource.getPath();
    }

    public String getName() {
        String result = definition.getName();
        if (StringUtils.isBlank(result)) {
            for (SnippetType type : SnippetType.values()) {
                if (StringUtils.startsWith(getId(), type.root() + "/")) {
                    result = StringUtils.removeEnd(StringUtils.removeStart(getId(), type.root() + "/"), FILE_EXTENSION);
                    result = StringUtils.replace(result, "/", "_");
                }
            }
        }
        if (StringUtils.isBlank(result)) {
            result = getId();
        }
        return result;
    }

    public String getGroup() {
        String result = StringUtils.trim(definition.getGroup());
        if (StringUtils.isBlank(result)) {
            result = StringUtils.substringBeforeLast(getPath(), "/");
        }
        return result;
    }

    @Override
    public int compareTo(Snippet o) {
        return this.getId().compareTo(o.getId());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("path", getPath())
                .toString();
    }
}
