package com.wttech.aem.contentor.core.snippet;

import com.wttech.aem.contentor.core.ContentorException;
import com.wttech.aem.contentor.core.util.ResourceSpliterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public class SnippetRepository {

  public static final String ROOT = "/conf/contentor/settings/snippet";

  private final ResourceResolver resourceResolver;

  public SnippetRepository(ResourceResolver resourceResolver) {
    this.resourceResolver = resourceResolver;
  }

  public Optional<Snippet> read(String path) {
    return Optional.ofNullable(path)
        .filter(p -> StringUtils.startsWith(p, SnippetType.AVAILABLE.root() + "/"))
        .map(resourceResolver::getResource)
        .flatMap(Snippet::from);
  }

  public Stream<Snippet> readAll(List<String> paths) {
    return paths.stream()
        .filter(p -> StringUtils.startsWith(p, SnippetType.AVAILABLE.root() + "/"))
        .map(resourceResolver::getResource)
        .map(Snippet::from)
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  public Stream<Snippet> findAll() throws ContentorException {
    return ResourceSpliterator.stream(getRoot())
        .map(Snippet::from)
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  private Resource getRoot() throws ContentorException {
    Resource root = resourceResolver.getResource(SnippetType.AVAILABLE.root());
    if (root == null) {
      throw new ContentorException(String.format("Snippets root path '%s' does not exist!", ROOT));
    }
    return root;
  }
}
