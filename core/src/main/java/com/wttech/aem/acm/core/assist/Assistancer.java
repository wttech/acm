package com.wttech.aem.acm.core.assist;

import com.wttech.aem.acm.core.AcmException;
import com.wttech.aem.acm.core.assist.resource.ResourceScanner;
import com.wttech.aem.acm.core.code.Variable;
import com.wttech.aem.acm.core.osgi.ClassInfo;
import com.wttech.aem.acm.core.osgi.OsgiScanner;
import com.wttech.aem.acm.core.snippet.SnippetRepository;
import com.wttech.aem.acm.core.util.SearchUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = Assistancer.class)
public class Assistancer {

    @Reference
    private transient OsgiScanner osgiScanner;

    @Reference
    private transient ResourceScanner resourceScanner;

    private BundleContext bundleContext;

    private BundleListener bundleListener;

    private List<ClassInfo> classCache = Collections.emptyList();

    private int bundlesHashCode;

    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.bundleListener = event -> updateCache();
        this.bundleContext.addBundleListener(bundleListener);

        updateCache();
    }

    @Modified
    protected void modified() {
        updateCache();
    }

    @Deactivate
    protected void deactivate() {
        if (bundleListener != null) {
            bundleContext.removeBundleListener(bundleListener);
        }
    }

    private synchronized void updateCache() {
        int bundlesHashCodeCurrent = osgiScanner.computeBundlesHashCode();
        if (bundlesHashCode != bundlesHashCodeCurrent) {
            classCache = osgiScanner.scanClasses().distinct().sorted().collect(Collectors.toList());
            bundlesHashCode = bundlesHashCodeCurrent;
        }
    }

    public Assistance forWord(ResourceResolver resolver, SuggestionType suggestionType, String word)
            throws AcmException {
        switch (suggestionType) {
            case VARIABLE:
                return new Assistance(variableSuggestions(word).collect(Collectors.toList()));
            case CLASS:
                return new Assistance(classSuggestions(word).collect(Collectors.toList()));
            case RESOURCE:
                return new Assistance(resourceSuggestions(resolver, word).collect(Collectors.toList()));
            case SNIPPET:
                return new Assistance(snippetSuggestions(resolver, word).collect(Collectors.toList()));
            default:
                return new Assistance(Stream.of(
                                classSuggestions(word),
                                resourceSuggestions(resolver, word),
                                variableSuggestions(word),
                                snippetSuggestions(resolver, word))
                        .flatMap(s -> s)
                        .collect(Collectors.toList()));
        }
    }

    private Stream<VariableSuggestion> variableSuggestions(String word) {
        return Arrays.stream(Variable.values())
                .filter(v -> SearchUtils.containsWord(v.varName(), word))
                .map(VariableSuggestion::new);
    }

    private Stream<SnippetSuggestion> snippetSuggestions(ResourceResolver resolver, String word) throws AcmException {
        return new SnippetRepository(resolver)
                .findAll()
                .filter(s -> SearchUtils.containsWord(s.getName(), word))
                .map(SnippetSuggestion::new);
    }

    private Stream<ClassSuggestion> classSuggestions(String word) {
        return classCache.stream()
                .filter(cf -> SearchUtils.containsWord(cf.getClassName(), word))
                .map(ClassSuggestion::new);
    }

    private Stream<Suggestion> resourceSuggestions(ResourceResolver resolver, String word) {
        return resourceScanner.forPattern(resolver, word).map(ResourceSuggestion::new);
    }
}
