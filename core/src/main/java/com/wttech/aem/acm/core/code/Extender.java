package com.wttech.aem.acm.core.code;

import groovy.lang.Script;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@Component(service = {Extender.class, ResourceChangeListener.class}, immediate = true)
public class Extender implements ResourceChangeListener {

    private final List<Script> scripts = new LinkedList<>();

    @Override
    public void onChange(@NotNull List<ResourceChange> list) {
        // TODO if any of the changes are related to the extension scripts, call compileExtensionScripts; but async; implement something like debounce
    }

    public void compileExtensionScripts() {
        // TODO ...
    }

    public Collection<Script> getScripts() {
        return scripts;
    }
}
