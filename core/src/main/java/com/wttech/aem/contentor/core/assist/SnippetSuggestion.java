package com.wttech.aem.contentor.core.assist;

import com.wttech.aem.contentor.core.snippet.Snippet;
import org.apache.commons.lang3.StringUtils;

public class SnippetSuggestion implements Suggestion {

  private final Snippet snippet;

  public SnippetSuggestion(Snippet snippet) {
    this.snippet = snippet;
  }

  @Override
  public String getKind() {
    return "snippet";
  }

  @Override
  public String getLabel() {
    return snippet.getName();
  }

  @Override
  public String getInsertText() {
    return snippet.getContent();
  }

  @Override
  public String getInfo() {
    if (StringUtils.isNotBlank(snippet.getDocumentation())) {
      return String.format("%s\n\nPath: %s", snippet.getDocumentation(), snippet.getPath());
    }
    return String.format("Path: %s", snippet.getPath());
  }
}
