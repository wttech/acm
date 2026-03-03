package dev.vml.es.acm.core.util;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.jcr.Node;
import javax.jcr.NodeIterator;

public class StreamUtils {

    public static <T> Stream<T> asStream(Iterator<T> sourceIterator) {
        return asStream(sourceIterator, false);
    }

    public static <T> Stream<T> asStream(Iterator<T> sourceIterator, boolean parallel) {
        Iterable<T> iterable = () -> sourceIterator;
        return StreamSupport.stream(iterable.spliterator(), parallel);
    }

    public static <T> Stream<T> asStream(Enumeration<T> enumeration) {
        if (enumeration == null) {
            return Stream.empty();
        }
        return asStream(new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return enumeration.hasMoreElements();
            }

            @Override
            public T next() {
                return enumeration.nextElement();
            }
        });
    }

    public static Stream<Node> asNodeStream(NodeIterator nodeIterator) {
        if (nodeIterator == null) {
            return Stream.empty();
        }
        return asStream(new Iterator<Node>() {
            @Override
            public boolean hasNext() {
                return nodeIterator.hasNext();
            }

            @Override
            public Node next() {
                return nodeIterator.nextNode();
            }
        });
    }
}
