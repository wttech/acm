package com.wttech.aem.contentor.core.assist;

import com.wttech.aem.contentor.core.util.DataStream;
import com.wttech.aem.contentor.core.util.DataStreams;

import java.io.Serializable;
import java.util.Collections;
import java.util.stream.Stream;

public class Assistance implements Serializable, DataStreams {

    private final Stream<Suggestion> suggestions;

    public Assistance(Stream<Suggestion> suggestions) {
        this.suggestions = suggestions;
    }

    @Override
    public Iterable<DataStream<?>> dataStreams() {
        return Collections.singletonList(DataStream.of("suggestions", suggestions));
    }
}
