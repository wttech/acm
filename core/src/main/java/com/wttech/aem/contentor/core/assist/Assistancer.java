package com.wttech.aem.contentor.core.assist;

import org.osgi.service.component.annotations.Component;

import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

@Component(immediate = true, service = Assistancer.class)
public class Assistancer {

    private static final Logger LOG = LoggerFactory.getLogger(Assistancer.class);

    @Reference
    private transient OsgiScanner osgiScanner;

    public Assistance forWord(String word) {
        LOG.info("Assisting for word '{}'", word);

        // TODO refactor osgi scanner to make it faster
        // TODO accept function to filter and map class names, map function should has an arg which BundleClass(clazz, bundle)
        List<String> classNames = osgiScanner.findClassNames(word);
        List<Suggestion> suggestions = new LinkedList<>();
        for (String className : classNames) {
            suggestions.add(new Suggestion("class", className));
        }

        LOG.info("Assisted for word '{}'. Found suggestions ({})", word, suggestions.size());

        return new Assistance(word, suggestions);
    }
}
