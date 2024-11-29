package com.wttech.aem.contentor.core.util;

import java.util.stream.Stream;

public interface DataStream<I> {

    String name();

    Stream<I> items();

    static <I> DataStream<I> of(String name, Stream<I> items) {
        return new DataStream<I>() {
            @Override
            public String name() {
                return name;
            }
            @Override
            public Stream<I> items() {
                return items;
            }
        };
    }
}
