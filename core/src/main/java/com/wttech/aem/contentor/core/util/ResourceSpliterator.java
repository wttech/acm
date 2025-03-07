package com.wttech.aem.contentor.core.util;

import java.util.Spliterator;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.sling.api.resource.Resource;

public class ResourceSpliterator implements Spliterator<Resource> {

    private final Stack<Resource> stack = new Stack<>();

    private final Predicate<Resource> traversePredicate;

    public ResourceSpliterator(Resource root) {
        this(root, resource -> true);
    }

    public ResourceSpliterator(Resource root, Predicate<Resource> traversePredicate) {
        this.traversePredicate = traversePredicate;
        if (this.traversePredicate.test(root)) {
            this.stack.push(root);
        }
    }

    public static Stream<Resource> stream(Resource root) {
        return StreamSupport.stream(new ResourceSpliterator(root), false);
    }

    public static Stream<Resource> stream(Resource root, Predicate<Resource> traversePredicate) {
        return StreamSupport.stream(new ResourceSpliterator(root, traversePredicate), false);
    }

    @Override
    public boolean tryAdvance(Consumer<? super Resource> action) {
        if (!stack.isEmpty()) {
            Resource current = stack.pop();
            action.accept(current);
            StreamSupport.stream(current.getChildren().spliterator(), false)
                    .filter(traversePredicate)
                    .forEach(stack::push);
            return true;
        }
        return false;
    }

    @Override
    public Spliterator<Resource> trySplit() {
        return null; // parallel processing is not supported
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE; // size is unknown
    }

    @Override
    public int characteristics() {
        return DISTINCT | NONNULL | IMMUTABLE;
    }
}
