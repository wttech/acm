package com.wttech.aem.contentor.core.script;

import com.wttech.aem.contentor.core.ContentorException;

public interface Executable {

    String getId();

    String getContent() throws ContentorException;
}
