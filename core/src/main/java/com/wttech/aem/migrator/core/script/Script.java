package com.wttech.aem.migrator.core.script;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wttech.aem.migrator.core.MigratorException;
import com.wttech.aem.migrator.core.util.JcrUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;

public class Script implements Executable {

    private final transient Resource resource;

    public Script(Resource resource) {
        this.resource = resource;
    }

    @Override
    public String getId() {
        return getPath();
    }

    @Override
    public String getContent() throws MigratorException {
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
