package com.wttech.aem.contentor.core.acl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants;

public class RestrictionOptions extends AuthorizableOptions {

    private String path;

    // TODO strongly natively-typed here, provide string-accepting utility setters
    private List<String> permissions = Collections.emptyList();

    private String glob;

    // TODO strongly natively-typed here, provide string-accepting utility setters
    private List<String> types = Collections.emptyList();

    // TODO strongly natively-typed here, provide string-accepting utility setters
    private List<String> properties = Collections.emptyList();

    // TODO strongly natively-typed here, provide string-accepting utility setters
    private Map<String, Object> restrictions = Collections.emptyMap();

    private Mode mode = Mode.SKIP;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public String getGlob() {
        return glob;
    }

    public void setGlob(String glob) {
        this.glob = glob;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    public List<String> getProperties() {
        return properties;
    }

    public void setProperties(List<String> properties) {
        this.properties = properties;
    }

    public Map<String, Object> getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(Map<String, Object> restrictions) {
        this.restrictions = restrictions;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Mode getMode() {
        return mode;
    }

    public void skipIfPathMissing() {
        mode = Mode.SKIP;
    }

    public void failIfPathMissing() {
        mode = Mode.FAIL;
    }

    public Map<String, Object> determineAllRestrictions() {
        Map<String, Object> allRestrictions = new HashMap<>();
        if (glob != null) {
            allRestrictions.put(AccessControlConstants.REP_GLOB, glob);
        }
        if (types != null && !types.isEmpty()) {
            allRestrictions.put(AccessControlConstants.REP_NT_NAMES, types);
        }
        if (properties != null && !properties.isEmpty()) {
            allRestrictions.put(AccessControlConstants.REP_ITEM_NAMES, properties);
        }
        if (restrictions != null && !restrictions.isEmpty()) {
            allRestrictions.putAll(restrictions);
        }
        return allRestrictions;
    }

    public enum Mode {
        FAIL,
        SKIP
    }
}
