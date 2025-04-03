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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        immediate = true,
        service = {Assistancer.class, EventHandler.class},
        property = {EventConstants.EVENT_TOPIC + "=" + Assistancer.UPDATE_CACHE_EVENT_TOPIC})
public class Assistancer implements EventHandler {

    public static final String UPDATE_CACHE_EVENT_TOPIC = "com/wttech/aem/acm/core/assist/UPDATE_CACHE";

    private static final Logger LOG = LoggerFactory.getLogger(Assistancer.class);

    @Reference
    private transient OsgiScanner osgiScanner;

    @Reference
    private transient ResourceScanner resourceScanner;

    @Reference
    private transient EventAdmin eventAdmin;

    private BundleContext bundleContext;

    private BundleListener bundleListener;

    private List<ClassInfo> classCache = Collections.emptyList();

    private int bundlesHashCode;

    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.bundleListener = event -> postUpdateCacheEvent();
        this.bundleContext.addBundleListener(bundleListener);

        postUpdateCacheEvent();
    }

    @Modified
    protected void modified() {
        postUpdateCacheEvent();
    }

    @Deactivate
    protected void deactivate() {
        if (bundleListener != null) {
            bundleContext.removeBundleListener(bundleListener);
        }
    }

    @Override
    public void handleEvent(Event event) {
        updateCache();
    }

    private void postUpdateCacheEvent() {
        eventAdmin.postEvent(new Event(UPDATE_CACHE_EVENT_TOPIC, Collections.emptyMap()));
    }

    private synchronized void updateCache() {
        int bundlesHashCodeCurrent = osgiScanner.computeBundlesHashCode();
        if (bundlesHashCode != bundlesHashCodeCurrent) {
            LOG.info("Bundles changed - updating cache");
            classCache = osgiScanner.scanClasses().distinct().sorted().collect(Collectors.toList());
            bundlesHashCode = bundlesHashCodeCurrent;
            LOG.info("Bundles changed - updated cache");
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
