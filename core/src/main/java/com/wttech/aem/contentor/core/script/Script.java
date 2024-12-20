package com.wttech.aem.contentor.core.script;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wttech.aem.contentor.core.code.Executable;
import com.wttech.aem.contentor.core.util.JcrUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.api.resource.Resource;

public class Script implements Executable, Comparable<Script> {

    public static final String EXTENSION = ".groovy";

    private final transient Resource resource;

    public Script(Resource resource) {
        this.resource = resource;
    }

    public static Optional<Script> from(Resource resource) {
        return Optional.ofNullable(resource).filter(Script::check).map(Script::new);
    }

    public static boolean check(Resource resource) {
        return resource != null
                && resource.isResourceType(JcrConstants.NT_FILE)
                && resource.getName().endsWith(EXTENSION);
    }

    @Override
    public String getId() {
        return getPath();
    }

    public String getName() {
        String result = getPath();
        result = StringUtils.removeStart(result, getType().root() + "/");
        result = StringUtils.removeEnd(result, EXTENSION);
        return result;
    }

    @Override
    public String getContent() throws com.wttech.aem.contentor.core.ContentorException {
        try {
            return IOUtils.toString(readContent(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ScriptException(String.format("Cannot read script as string '%s'!", getPath()), e);
        }
    }

    @JsonIgnore
    public String getPath() {
        return resource.getPath();
    }

    @JsonIgnore
    public ScriptType getType() {
        return ScriptType.byPath(getPath()).orElse(ScriptType.DISABLED);
    }

    public InputStream readContent() throws ScriptException {
        return Optional.ofNullable(resource.getChild(JcrUtils.JCR_CONTENT))
                .map(r -> r.adaptTo(InputStream.class))
                .orElseThrow(() -> new ScriptException(String.format("Cannot read script '%s'!", getPath())));
    }

    protected Resource getResource() {
        return resource;
    }

    @Override
    public int compareTo(Script other) {
        if (other == null) {
            return 1;
        }
        return this.getPath().compareTo(other.getPath());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("path", getPath())
                .toString();
    }
}
