package com.wttech.aem.contentor.core.util;

import java.util.Spliterator;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.sling.api.resource.Resource;

public class ResourceSpliterator implements Spliterator<Resource> {

  private final Stack<Resource> stack = new Stack<>();

  public ResourceSpliterator(Resource root) {
    stack.push(root);
  }

  public static Stream<Resource> stream(Resource root) {
    return StreamSupport.stream(new ResourceSpliterator(root), false);
  }

  @Override
  public boolean tryAdvance(Consumer<? super Resource> action) {
    if (!stack.isEmpty()) {
      Resource current = stack.pop();
      action.accept(current);
      current.listChildren().forEachRemaining(stack::push);
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
