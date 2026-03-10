package dev.vml.es.acm.core.util;

import static javax.jcr.Property.JCR_CONTENT;
import static javax.jcr.Property.JCR_DATA;
import static javax.jcr.Property.JCR_FROZEN_PRIMARY_TYPE;
import static javax.jcr.nodetype.NodeType.NT_FILE;
import static javax.jcr.nodetype.NodeType.NT_FROZEN_NODE;
import static javax.jcr.nodetype.NodeType.NT_LINKED_FILE;

import java.util.Iterator;
import java.util.stream.Stream;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

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

    /**
     * Returns the binary data property of the given node (e.g. for nt:file, nt:linkedFile, dam:Asset).
     * Copied from internal Sling class.
     *
     * @see <a href="https://github.com/apache/sling-org-apache-sling-jcr-resource/blob/master/src/main/java/org/apache/sling/jcr/resource/internal/NodeUtil.java">NodeUtil#getPrimaryProperty</a>
     */
    public static Property getBinaryDataProperty(Node node) throws RepositoryException {
        Node content = (node.isNodeType(NT_FILE)
                        || (node.isNodeType(NT_FROZEN_NODE)
                                && node.getProperty(JCR_FROZEN_PRIMARY_TYPE)
                                        .getString()
                                        .equals(NT_FILE)))
                ? node.getNode(JCR_CONTENT)
                : node.isNodeType(NT_LINKED_FILE)
                        ? node.getProperty(JCR_CONTENT).getNode()
                        : node;
        if (content.hasProperty(JCR_DATA)) {
            return content.getProperty(JCR_DATA);
        }
        Item item = content.getPrimaryItem();
        while (item.isNode()) {
            item = ((Node) item).getPrimaryItem();
        }
        return (Property) item;
    }

    public static boolean hasBinaryData(Node node) {
        if (node == null) {
            return false;
        }
        try {
            return getBinaryDataProperty(node) != null;
        } catch (RepositoryException e) {
            return false;
        }
    }
}
