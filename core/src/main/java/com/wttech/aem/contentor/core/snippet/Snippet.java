package com.wttech.aem.contentor.core.snippet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wttech.aem.contentor.core.code.Executable;
import com.wttech.aem.contentor.core.script.ScriptException;
import com.wttech.aem.contentor.core.util.JcrUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.api.resource.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class Snippet implements Executable {

    private final transient Resource resource;

    public Snippet(Resource resource) {
        this.resource = resource;
    }

    public static Optional<Snippet> from(Resource resource) {
        return Optional.ofNullable(resource)
                .filter(Snippet::check)
                .map(Snippet::new);
    }

    public static boolean check(Resource resource) {
        return resource != null && resource.isResourceType(JcrConstants.NT_FILE) && resource.getName().endsWith(".txt");
    }

    @Override
    public String getId() {
        return getPath();
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

    public InputStream readContent() throws ScriptException {
        return Optional.ofNullable(resource.getChild(JcrUtils.JCR_CONTENT))
                .map(r -> r.adaptTo(InputStream.class))
                .orElseThrow(() -> new ScriptException(String.format("Cannot read script '%s'!", getPath())));
    }
}
