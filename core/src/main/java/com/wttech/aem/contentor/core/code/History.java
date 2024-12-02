package com.wttech.aem.contentor.core.code;

import org.osgi.service.component.annotations.Component;

import java.util.stream.Stream;

@Component(immediate = true, service = History.class)
public class History {

    public Stream<Execution> read() {
        return Stream.empty();
    }

    public void save(Execution execution) {
        // TODO
    }
}
