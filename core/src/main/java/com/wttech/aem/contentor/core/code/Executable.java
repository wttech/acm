package com.wttech.aem.contentor.core.code;

import com.wttech.aem.contentor.core.ContentorException;
import java.io.Serializable;

public interface Executable extends Serializable {

  String getId();

  String getContent() throws ContentorException;
}
