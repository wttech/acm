package com.wttech.aem.contentor.core.assist;

import org.osgi.service.component.annotations.Component;

import org.osgi.service.component.annotations.Reference;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Component(immediate = true, service = Assistancer.class)
public class Assistancer {

    @Reference
    private transient OsgiScanner osgiScanner;

    public Assistance all() {
        List<Suggestion> suggestions = osgiScanner.scanClasses()
                .map(this::classSuggestion)
                .collect(Collectors.toCollection(LinkedList::new));
        return new Assistance(suggestions);
    }

    private Suggestion classSuggestion(BundleClass bundleClass) {
        return new Suggestion("class",
                bundleClass.getClassName(),
                String.format("Bundle: %s", bundleClass.getBundleSymbolicName()));
    }
}
