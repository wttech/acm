package com.wttech.aem.contentor.core.assist;

import com.wttech.aem.contentor.core.osgi.ClassInfo;
import com.wttech.aem.contentor.core.osgi.OsgiScanner;
import com.wttech.aem.contentor.core.util.SearchUtils;
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

    private List<ClassInfo> classSuggestions = Collections.emptyList();

    @Activate
    @Modified
    protected void activate() {
        this.classSuggestions = osgiScanner.scanClasses().sorted().collect(Collectors.toList());
    }

    public Assistance forWord(String word) {
        return new Assistance(
                classSuggestions.stream()
                .filter(cf -> SearchUtils.containsWord(cf.getClassName(), word))
                .map(ClassSuggestion::new)
                .collect(Collectors.toList())
        );
    }
}
