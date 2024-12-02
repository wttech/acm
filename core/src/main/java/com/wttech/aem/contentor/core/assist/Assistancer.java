package com.wttech.aem.contentor.core.assist;

import com.wttech.aem.contentor.core.assist.osgi.ClassInfo;
import com.wttech.aem.contentor.core.assist.osgi.OsgiScanner;
import com.wttech.aem.contentor.core.assist.resource.ResourceScanner;
import com.wttech.aem.contentor.core.code.Variable;
import com.wttech.aem.contentor.core.util.SearchUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(immediate = true, service = Assistancer.class)
public class Assistancer {

    @Reference
    private transient OsgiScanner osgiScanner;

    @Reference
    private transient ResourceScanner resourceScanner;

    private List<ClassInfo> classCache = Collections.emptyList();

    @Activate
    @Modified
    protected void activate() {
        // TODO invalidate it based on bundles checksums (lastModified + symbolicName + version)
        this.classCache = osgiScanner.scanClasses().distinct().sorted().collect(Collectors.toList());
    }

    public Assistance forWord(ResourceResolver resolver, SuggestionType suggestionType, String word) {
        switch (suggestionType) {
            case VARIABLE:
                return new Assistance(variableSuggestions(word).collect(Collectors.toList()));
            case CLASS:
                return new Assistance(classSuggestions(word).collect(Collectors.toList()));
            case RESOURCE:
                return new Assistance(resourceSuggestions(resolver, word).collect(Collectors.toList()));
            default:
                return new Assistance(Stream.of(
                        classSuggestions(word),
                        resourceSuggestions(resolver, word),
                        variableSuggestions(word)
                ).flatMap(s -> s).collect(Collectors.toList()));
        }
    }

    private Stream<VariableSuggestion> variableSuggestions(String word) {
        return Arrays.stream(Variable.values())
                .filter(v -> SearchUtils.containsWord(v.varName(), word))
                .map(VariableSuggestion::new);
    }

    private Stream<ClassSuggestion> classSuggestions(String word) {
        return classCache.stream()
                .filter(cf -> SearchUtils.containsWord(cf.getClassName(), word))
                .map(ClassSuggestion::new);
    }

    private Stream<Suggestion> resourceSuggestions(ResourceResolver resolver, String word) {
        return resourceScanner.forPattern(resolver, word)
                .map(ResourceSuggestion::new);
    }
}
