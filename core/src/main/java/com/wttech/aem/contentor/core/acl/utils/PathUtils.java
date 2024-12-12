package com.wttech.aem.contentor.core.acl.utils;

import org.apache.commons.lang3.StringUtils;

public final class PathUtils {

    private PathUtils() {
        // intentionally empty
    }

    public static boolean isAppsOrLibsPath(String path) {
        return StringUtils.startsWithAny(path, "/apps", "/libs");
    }
}
