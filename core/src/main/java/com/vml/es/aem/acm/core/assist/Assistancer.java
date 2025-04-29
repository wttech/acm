package com.vml.es.aem.acm.core.assist;

import com.vml.es.aem.acm.core.AcmException;
import com.vml.es.aem.acm.core.assist.resource.ResourceScanner;
import com.vml.es.aem.acm.core.code.*;
import com.vml.es.aem.acm.core.osgi.ClassInfo;
import com.vml.es.aem.acm.core.osgi.OsgiScanner;
import com.vml.es.aem.acm.core.snippet.SnippetRepository;
import com.vml.es.aem.acm.core.util.SearchUtils;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = Assistancer.class)
@Designate(ocd = Assistancer.Config.class)
public class Assistancer {

    private static final Logger LOG = LoggerFactory.getLogger(Assistancer.class);

    @ObjectClassDefinition(name = "AEM Content Manager - Assistancer")
    public @interface Config {

        @AttributeDefinition(
                name = "Cache Max Age - All",
                description = "Seconds to cache in browser responses for 'all' assistance type")
        int cacheMaxAgeAll() default 60 * 5;

        @AttributeDefinition(
                name = "Cache Max Age - Specific",
                description = "Seconds to cache in browser responses for specific assistance type")
        int cacheMaxAgeSpecific() default 10;

        @AttributeDefinition(name = "Cache Lifetime - Variables", description = "Seconds to cache them in-memory")
        int cacheLifetimeVariables() default 60 * 5;
    }

    private Config config;

    @Reference
    private transient OsgiScanner osgiScanner;

    @Reference
    private transient ResourceScanner resourceScanner;

    @Reference
    private Executor executor;

    @Reference
    private CodeRepository codeRepository;

    private List<ClassInfo> javaClassCache;

    private List<ClassInfo> bundleClassCache = Collections.emptyList();

    private Integer bundleClassCacheHashCode;

    private List<Variable> variablesCache = Collections.emptyList();

    private Long variablesCacheTimestamp;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
    }

    public Assistance forWord(ResourceResolver resolver, SuggestionType suggestionType, String word)
            throws AcmException {
        maybeLoadJavaClassCache(resolver);
        maybeUpdateBundleClassCache();
        maybeUpdateVariablesCache(resolver);

        List<Suggestion> suggestions = new LinkedList<>();
        switch (suggestionType) {
            case VARIABLE:
                suggestions.addAll(variableSuggestions(word).collect(Collectors.toList()));
                break;
            case CLASS:
                suggestions.addAll(javaClassSuggestions(word).collect(Collectors.toList()));
                suggestions.addAll(bundleClassSuggestions(word).collect(Collectors.toList()));
                break;
            case RESOURCE:
                suggestions.addAll(resourceSuggestions(resolver, word).collect(Collectors.toList()));
                break;
            case SNIPPET:
                suggestions.addAll(snippetSuggestions(resolver, word).collect(Collectors.toList()));
                break;
            default:
                suggestions.addAll(variableSuggestions(word).collect(Collectors.toList()));
                suggestions.addAll(javaClassSuggestions(word).collect(Collectors.toList()));
                suggestions.addAll(bundleClassSuggestions(word).collect(Collectors.toList()));
                suggestions.addAll(resourceSuggestions(resolver, word).collect(Collectors.toList()));
                suggestions.addAll(snippetSuggestions(resolver, word).collect(Collectors.toList()));
                break;
        }
        return new Assistance(suggestions);
    }

    private void maybeLoadJavaClassCache(ResourceResolver resolver) {
        if (javaClassCache == null) {
            LOG.info("Java class cache - loading");
            Bundle bundle = osgiScanner.getSystemBundle();
            this.javaClassCache = JavaClassDictionary.determine(resolver)
                    .getClasses()
                    .map(className -> new ClassInfo(className, bundle))
                    .collect(Collectors.toList());
            LOG.info("Java class cache - loaded");
        }
    }

    private Stream<VariableSuggestion> variableSuggestions(String word) {
        return variablesCache.stream()
                .filter(v -> SearchUtils.containsWord(v.getName(), word))
                .map(cf -> new VariableSuggestion(cf, codeRepository));
    }

    private Stream<SnippetSuggestion> snippetSuggestions(ResourceResolver resolver, String word) throws AcmException {
        return new SnippetRepository(resolver)
                .findAll()
                .filter(s -> SearchUtils.containsWord(s.getName(), word))
                .map(SnippetSuggestion::new);
    }

    private Stream<ClassSuggestion> javaClassSuggestions(String word) {
        return javaClassCache.stream()
                .filter(cf -> SearchUtils.containsWord(cf.getClassName(), word))
                .map(cf -> new ClassSuggestion(cf, codeRepository));
    }

    private Stream<ClassSuggestion> bundleClassSuggestions(String word) {
        return bundleClassCache.stream()
                .filter(cf -> SearchUtils.containsWord(cf.getClassName(), word))
                .map(cf -> new ClassSuggestion(cf, codeRepository));
    }

    private Stream<Suggestion> resourceSuggestions(ResourceResolver resolver, String word) {
        return resourceScanner.forPattern(resolver, word).map(ResourceSuggestion::new);
    }

    private synchronized void maybeUpdateBundleClassCache() {
        int cacheHashCodeCurrent = osgiScanner.computeBundlesHashCode();
        if (bundleClassCacheHashCode == null || !bundleClassCacheHashCode.equals(cacheHashCodeCurrent)) {
            LOG.info("Bundle class cache - updating");
            bundleClassCache =
                    osgiScanner.scanExportedClasses().distinct().sorted().collect(Collectors.toList());
            bundleClassCacheHashCode = cacheHashCodeCurrent;
            LOG.info("Bundle class cache - updated");
        }
    }

    private synchronized void maybeUpdateVariablesCache(ResourceResolver resolver) {
        long currentTime = System.currentTimeMillis();
        long cacheLifetimeMillis = config.cacheLifetimeVariables() * 1000L;

        if (variablesCacheTimestamp == null || (currentTime - variablesCacheTimestamp) > cacheLifetimeMillis) {
            LOG.info("Variables cache - updating");
            try (ExecutionContext context = executor.createContext(
                    ExecutionId.generate(), ExecutionMode.PARSE, Code.consoleMinimal(), resolver)) {
                variablesCache = context.getBindingVariables();
                variablesCacheTimestamp = currentTime;
            }
            LOG.info("Variables cache - updated");
        }
    }

    public int getCacheMaxAgeAll() {
        return config.cacheMaxAgeAll();
    }

    public int getCacheMaxAgeSpecific() {
        return config.cacheMaxAgeSpecific();
    }
}
