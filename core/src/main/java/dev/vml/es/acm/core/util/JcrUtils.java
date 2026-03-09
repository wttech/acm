package dev.vml.es.acm.core.util;

import java.util.Iterator;
import java.util.stream.Stream;
import javax.jcr.Node;
import javax.jcr.NodeIterator;

public class JcrUtils {

    public static Stream<Node> asNodeStream(NodeIterator nodeIterator) {
        if (nodeIterator == null) {
            return Stream.empty();
        }
        return StreamUtils.asStream(new Iterator<Node>() {
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
