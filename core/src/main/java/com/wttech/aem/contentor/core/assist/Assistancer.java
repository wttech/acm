package com.wttech.aem.contentor.core.assist;

import com.wttech.aem.contentor.core.osgi.OsgiScanner;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component(immediate = true, service = Assistancer.class)
public class Assistancer {

    @Reference
    private transient OsgiScanner osgiScanner;

    private List<Suggestion> classSuggestions = Collections.emptyList();

    @Activate
    @Modified
    protected void activate() {
        this.classSuggestions = osgiScanner.scanClasses().map(ClassSuggestion::new).collect(Collectors.toList());
    }

    public Assistance all() {
        return new Assistance(classSuggestions);
    }
}
