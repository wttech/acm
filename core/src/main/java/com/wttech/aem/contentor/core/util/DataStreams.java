package com.wttech.aem.contentor.core.util;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface DataStreams {

    @JsonIgnore
    Iterable<DataStream<?>> dataStreams();
}
