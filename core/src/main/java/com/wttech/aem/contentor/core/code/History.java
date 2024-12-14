package com.wttech.aem.contentor.core.code;

import java.util.stream.Stream;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = History.class)
public class History {

  public Stream<Execution> read() {
    return Stream.empty();
  }

  public void save(Execution execution) {
    // TODO
  }

  public boolean contains(String id) {
    return false;
  }
}
