package com.wttech.aem.contentor.core.assist;

import com.wttech.aem.contentor.core.assist.osgi.BundleScanner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import java.util.LinkedList;
import java.util.List;

@Component(immediate = true, service = Assistancer.class)
public class Assistancer {

    private transient BundleContext bundleContext;

    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public Assistance forWord(String word) {
        List<Suggestion> suggestions = new LinkedList<>();
        for (Bundle bundle : bundleContext.getBundles()) {
            for (String className : new BundleScanner(bundle).findClassNames(word)) {
                suggestions.add(new Suggestion("class", className));
            }
        }
        return new Assistance(word, suggestions);
    }
}
