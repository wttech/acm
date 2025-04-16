package com.vml.es.aem.acm.core.acl.authorizable;

import com.vml.es.aem.acm.core.acl.utils.PrivilegeGroup;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants;

public class PermissionsOptions {

    private String path;

    private List<String> permissions = Collections.emptyList();

    private String glob;

    private List<String> types;

    private List<String> properties;

    private Map<String, Object> restrictions;

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

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void skipIfPathMissing() {
        mode = Mode.SKIP;
    }

    public void failIfPathMissing() {
        mode = Mode.FAIL;
    }

    public List<String> determineAllPermissions() {
        return permissions.stream()
                .map(PrivilegeGroup::determinePermissions)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    public Map<String, Object> determineAllRestrictions() {
        Map<String, Object> allRestrictions = new HashMap<>();
        allRestrictions.compute(AccessControlConstants.REP_GLOB, (key, value) -> glob);
        allRestrictions.compute(AccessControlConstants.REP_NT_NAMES, (key, value) -> types);
        allRestrictions.compute(AccessControlConstants.REP_ITEM_NAMES, (key, value) -> properties);
        if (restrictions != null) {
            allRestrictions.putAll(restrictions);
        }
        return allRestrictions;
    }

    public enum Mode {
        FAIL,
        SKIP
    }
}
