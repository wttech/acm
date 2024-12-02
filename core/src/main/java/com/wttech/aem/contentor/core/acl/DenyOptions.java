package com.wttech.aem.contentor.core.acl;

import java.util.Collection;

public class DenyOptions {

    private String path;

    private Collection<String> permissions;

    public static DenyOptions simple(String path, Collection<String> permissions) {
        DenyOptions result = new DenyOptions();
        result.path = path;
        result.permissions = permissions;
        return result;
    }
}
