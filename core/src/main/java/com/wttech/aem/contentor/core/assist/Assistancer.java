package com.wttech.aem.contentor.core.assist;

import com.wttech.aem.contentor.core.util.SearchUtils;
import org.osgi.service.component.annotations.Component;

import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Component(immediate = true, service = Assistancer.class)
public class Assistancer {

    private static final Logger LOG = LoggerFactory.getLogger(Assistancer.class);

    @Reference
    private transient OsgiScanner osgiScanner;

    public Assistance forWord(String word, int limit) {
        LOG.info("Assisting for word '{}'", word);

        // TODO refactor osgi scanner to make it faster
        // TODO accept function to filter and map class names, map function should has an arg which BundleClass(clazz, bundle)
        // TODO cache all BundleClass instances ; cache should be per bundle; checksum invalidated when bundle.symbolicName and lastModified() changes
        // TODO always lookup all available bundles but reuse cached BundleClass instances
        List<Suggestion> suggestions = osgiScanner.allClasses()
                .filter(bundleClass -> SearchUtils.containsWord(bundleClass.getClassName(), word))
                .map(this::classSuggestion)
                // order by levenstein distance or sth sophisticated
                .collect(Collectors.toCollection(LinkedList::new));

        LOG.info("Assisted for word '{}'. Found suggestions ({})", word, suggestions.size());

        return new Assistance(word, suggestions);
    }

    private Suggestion classSuggestion(BundleClass bundleClass) {
        return new Suggestion("class",
                bundleClass.getClassName(),
                String.format("Bundle: %s", bundleClass.getBundleSymbolicName()));
    }
}
