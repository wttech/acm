package com.wttech.aem.migrator.core.pkg;

import java.util.List;

public class Package {

    private final String pid;

    public Package(String pid) {
        this.pid = pid;
    }

    public String getPid() {
        return pid;
    }

    public List<String> findScriptPaths() {
        return List.of();
    }
}
