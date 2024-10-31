package com.wttech.aem.migrator.core.instance;

import com.wttech.aem.migrator.core.script.Queue;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

@Component(immediate = true, service = EventHandler.class)
public class PackageListener implements EventHandler {

    @Reference
    private Queue queue;

    @Override
    public void handleEvent(Event event) {
        // TODO watch for AEM package installations containing migrator scripts
        // TODO add to the queue all scripts from the package
    }
}
