package com.wttech.aem.contentor.core.acl;

import java.util.Collection;

public class AllowOptions {

    private String path;

    private Collection<String> permissions;

    public static AllowOptions simple(String path, Collection<String> permissions) {
        AllowOptions result = new AllowOptions();
        result.path = path;
        result.permissions = permissions;
        return result;
    }

    // TODO ...
}
