package com.wttech.aem.acm.core.acl;

import java.util.Arrays;

public enum AclResult {
    OK,
    CHANGED,
    SKIPPED;

    public static AclResult of(AclResult... results) {
        if (Arrays.stream(results).allMatch(result -> result == SKIPPED)) {
            return SKIPPED;
        }
        return Arrays.stream(results).anyMatch(result -> result == CHANGED) ? CHANGED : OK;
    }
}
