package com.wttech.aem.acm.core.assist;

import com.wttech.aem.acm.core.AcmException;
import com.wttech.aem.acm.core.assist.resource.ResourceScanner;
import com.wttech.aem.acm.core.code.*;
import com.wttech.aem.acm.core.osgi.ClassInfo;
import com.wttech.aem.acm.core.osgi.OsgiScanner;
import com.wttech.aem.acm.core.snippet.SnippetRepository;
import com.wttech.aem.acm.core.util.SearchUtils;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = Assistancer.class)
public class Assistancer {

    private static final Logger LOG = LoggerFactory.getLogger(Assistancer.class);

    @Reference
    private transient OsgiScanner osgiScanner;

    @Reference
    private transient ResourceScanner resourceScanner;

    @Reference
    private Executor executor;

    private List<ClassInfo> classCache = Collections.emptyList();

    private Integer cacheHashCode;

    public Assistance forWord(ResourceResolver resolver, SuggestionType suggestionType, String word)
            throws AcmException {
        maybeUpdateCache();

        switch (suggestionType) {
            case VARIABLE:
                return new Assistance(variableSuggestions(resolver, word).collect(Collectors.toList()));
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
                                variableSuggestions(resolver, word),
                                snippetSuggestions(resolver, word))
                        .flatMap(s -> s)
                        .collect(Collectors.toList()));
        }
    }

    private Stream<VariableSuggestion> variableSuggestions(ResourceResolver resolver, String word) {
        try (ExecutionContext context =
                executor.createContext(ExecutionId.generate(), Code.consoleMinimal(), resolver)) {
            return context.getBindingVariables().stream()
                    .filter(v -> SearchUtils.containsWord(v.getName(), word))
                    .map(VariableSuggestion::new);
        }
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

    private synchronized void maybeUpdateCache() {
        int cacheHashCodeCurrent = osgiScanner.computeBundlesHashCode();
        if (cacheHashCode == null || !cacheHashCode.equals(cacheHashCodeCurrent)) {
            LOG.info("Bundles changed - updating cache");
            classCache = osgiScanner.scanClasses().distinct().sorted().collect(Collectors.toList());
            cacheHashCode = cacheHashCodeCurrent;
            LOG.info("Bundles changed - updated cache");
        }
    }
}
