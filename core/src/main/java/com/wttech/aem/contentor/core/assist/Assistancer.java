package com.wttech.aem.contentor.core.assist;

import org.osgi.service.component.annotations.Component;

import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = Assistancer.class)
public class Assistancer {

    @Reference
    private transient OsgiScanner osgiScanner;

    public Assistance all() {
        return new Assistance(osgiScanner.scanClasses().map(this::classSuggestion));
    }

    private Suggestion classSuggestion(BundleClass bundleClass) {
        return new Suggestion("class", bundleClass.getClassName(), String.format("Bundle: %s", bundleClass.getBundleSymbolicName()));
    }
}
