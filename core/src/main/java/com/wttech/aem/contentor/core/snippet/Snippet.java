package com.wttech.aem.contentor.core.snippet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wttech.aem.contentor.core.ContentorException;
import com.wttech.aem.contentor.core.code.Executable;
import com.wttech.aem.contentor.core.util.JcrUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.api.resource.Resource;

import java.io.InputStream;
import java.util.Optional;

public class Snippet implements Executable, Comparable<Snippet> {

    public static final String FILE_EXTENSION = ".yml";

    private final transient Resource resource;

    private final transient SnippetDefinition definition;

    public Snippet(Resource resource) throws ContentorException {
        this.resource = resource;
        this.definition = readDefinition();
    }

    public static Optional<Snippet> from(Resource resource) throws ContentorException {
        return Optional.ofNullable(resource)
                .filter(Snippet::check)
                .map(Snippet::new);
    }

    public static boolean check(Resource resource) {
        return resource != null && resource.isResourceType(JcrConstants.NT_FILE) && resource.getName().endsWith(FILE_EXTENSION);
    }

    private SnippetDefinition readDefinition() throws ContentorException {
        return Optional.ofNullable(resource.getChild(JcrUtils.JCR_CONTENT))
                .map(r -> r.adaptTo(InputStream.class))
                .map(SnippetDefinition::fromYaml)
                .orElseThrow(() -> new ContentorException(String.format("Snippet definition '%s' cannot be read found!", resource.getPath())));
    }

    @Override
    public String getId() {
        return getPath();
    }

    @Override
    public String getContent() throws ContentorException {
        return definition.getContent();
    }

    public String getDocumentation() throws ContentorException {
        return definition.getDocumentation();
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
                }
            }
        }
        if (StringUtils.isBlank(result)) {
            result = getId();
        }
        return result;
    }

    @Override
    public int compareTo(Snippet o) {
        return this.getName().compareTo(o.getName());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("path", getPath())
                .toString();
    }
}
