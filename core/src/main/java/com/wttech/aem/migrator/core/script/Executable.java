package com.wttech.aem.migrator.core.script;

import com.wttech.aem.migrator.core.MigratorException;

public interface Executable {

    String getId();

    String getContent() throws MigratorException;
}
