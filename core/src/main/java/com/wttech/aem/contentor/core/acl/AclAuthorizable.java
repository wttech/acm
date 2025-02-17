package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.ClearOptions;
import com.wttech.aem.contentor.core.acl.authorizable.GroupOptions;
import com.wttech.aem.contentor.core.acl.authorizable.PermissionsOptions;
import com.wttech.aem.contentor.core.acl.authorizable.RemovePropertyOptions;
import com.wttech.aem.contentor.core.acl.authorizable.SetPropertyOptions;
import com.wttech.aem.contentor.core.acl.utils.PathUtils;
import com.wttech.aem.contentor.core.util.GroovyUtils;
import groovy.lang.Closure;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.jcr.RepositoryException;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;

public class AclAuthorizable {

    private static final String EVERYONE = "everyone";

    private final Authorizable authorizable;

    private final String id;

    protected final AclContext context;

    public AclAuthorizable(Authorizable authorizable, String id, AclContext context) {
        this.authorizable = authorizable;
        this.id = id;
        this.context = context;
    }

    public AclResult addToGroup(Closure<com.wttech.aem.contentor.core.acl.authorizable.GroupOptions> closure) {
        return addToGroup(GroovyUtils.with(new com.wttech.aem.contentor.core.acl.authorizable.GroupOptions(), closure));
    }

    public AclResult removeFromGroup(Closure<com.wttech.aem.contentor.core.acl.authorizable.GroupOptions> closure) {
        return removeFromGroup(
                GroovyUtils.with(new com.wttech.aem.contentor.core.acl.authorizable.GroupOptions(), closure));
    }

    public AclResult removeFromAllGroups(Closure<Void> closure) {
        return removeFromAllGroups();
    }

    public AclResult clear(Closure<com.wttech.aem.contentor.core.acl.authorizable.ClearOptions> closure) {
        return clear(GroovyUtils.with(new com.wttech.aem.contentor.core.acl.authorizable.ClearOptions(), closure));
    }

    public AclResult purge(Closure<Void> closure) {
        return purge();
    }

    public AclResult allow(Closure<com.wttech.aem.contentor.core.acl.authorizable.PermissionsOptions> closure) {
        return allow(
                GroovyUtils.with(new com.wttech.aem.contentor.core.acl.authorizable.PermissionsOptions(), closure));
    }

    public AclResult deny(Closure<com.wttech.aem.contentor.core.acl.authorizable.PermissionsOptions> closure) {
        return deny(GroovyUtils.with(new com.wttech.aem.contentor.core.acl.authorizable.PermissionsOptions(), closure));
    }

    public AclResult setProperty(Closure<com.wttech.aem.contentor.core.acl.authorizable.SetPropertyOptions> closure) {
        return setProperty(
                GroovyUtils.with(new com.wttech.aem.contentor.core.acl.authorizable.SetPropertyOptions(), closure));
    }

    public AclResult removeProperty(
            Closure<com.wttech.aem.contentor.core.acl.authorizable.RemovePropertyOptions> closure) {
        return removeProperty(
                GroovyUtils.with(new com.wttech.aem.contentor.core.acl.authorizable.RemovePropertyOptions(), closure));
    }

    public AclResult addToGroup(com.wttech.aem.contentor.core.acl.authorizable.GroupOptions options) {
        AclGroup group = context.determineGroup(options.getGroup(), options.getGroupId());
        return addToGroup(group);
    }

    public AclResult addToGroup(String groupId) {
        AclGroup group = context.determineGroup(groupId);
        return addToGroup(group);
    }

    public AclResult addToGroup(AclGroup group) {
        AclResult result;
        if (authorizable == null) {
            result = AclResult.SKIPPED;
        } else if (group.get() == null) {
            result = AclResult.SKIPPED;
        } else {
            result = context.getAuthorizableManager().addMember(group.get(), authorizable)
                    ? AclResult.CHANGED
                    : AclResult.OK;
        }
        context.logResult(this, "addToGroup {} {}", result, group.getId());
        return result;
    }

    public AclResult removeFromGroup(GroupOptions options) {
        AclGroup group = context.determineGroup(options.getGroup(), options.getGroupId());
        return removeFromGroup(group);
    }

    public AclResult removeFromGroup(String groupId) {
        AclGroup group = context.determineGroup(groupId);
        return removeFromGroup(group);
    }

    public AclResult removeFromGroup(AclGroup group) {
        AclResult result;
        if (authorizable == null) {
            result = AclResult.SKIPPED;
        } else if (group.get() == null) {
            result = AclResult.SKIPPED;
        } else {
            result = context.getAuthorizableManager().removeMember(group.get(), authorizable)
                    ? AclResult.CHANGED
                    : AclResult.OK;
        }
        context.logResult(this, "removeFromGroup {} {}", result, group.getId());
        return result;
    }

    public AclResult removeFromAllGroups() {
        try {
            AclResult result;
            if (authorizable == null) {
                result = AclResult.SKIPPED;
            } else {
                Iterator<Group> groups = authorizable.memberOf();
                result = AclResult.OK;
                while (groups.hasNext()) {
                    Group group = groups.next();
                    if (!StringUtils.equals(group.getID(), EVERYONE)
                            && context.getAuthorizableManager().removeMember(group, authorizable)) {
                        result = AclResult.CHANGED;
                    }
                }
            }
            context.logResult(this, "removeFromAllGroups {}", result);
            return result;
        } catch (RepositoryException e) {
            throw new AclException("Failed to remove authorizable from all groups", e);
        }
    }

    public AclResult clear(ClearOptions options) {
        return clear(options.getPath(), options.isStrict());
    }

    public AclResult clear(String path, boolean strict) {
        AclResult result;
        if (authorizable == null) {
            result = AclResult.SKIPPED;
        } else if (context.isCompositeNodeStore() && PathUtils.isAppsOrLibsPath(path)) {
            result = AclResult.SKIPPED;
        } else if (context.getResourceResolver().getResource(path) == null) {
            result = AclResult.SKIPPED;
        } else {
            result = context.getPermissionsManager().clear(authorizable, path, strict)
                    ? AclResult.CHANGED
                    : AclResult.OK;
        }
        context.logResult(this, "clear {} {}", result, path);
        return result;
    }

    public AclResult clear(String path) {
        return clear(path, false);
    }

    public AclResult purge() {
        return AclResult.SKIPPED;
    }

    private AclResult apply(
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            com.wttech.aem.contentor.core.acl.authorizable.PermissionsOptions.Mode mode,
            boolean allow) {
        com.wttech.aem.contentor.core.acl.authorizable.PermissionsOptions options =
                new com.wttech.aem.contentor.core.acl.authorizable.PermissionsOptions();
        options.setPermissions(permissions);
        options.setGlob(glob);
        options.setTypes(types);
        options.setProperties(properties);
        options.setRestrictions(restrictions);
        AclResult result;
        if (authorizable == null) {
            result = AclResult.SKIPPED;
        } else if (context.isCompositeNodeStore() && PathUtils.isAppsOrLibsPath(path)) {
            result = AclResult.SKIPPED;
        } else if (context.getResourceResolver().getResource(path) == null) {
            if (mode == com.wttech.aem.contentor.core.acl.authorizable.PermissionsOptions.Mode.FAIL) {
                throw new AclException(String.format("Path %s not found", path));
            }
            result = AclResult.SKIPPED;
        } else {
            result = apply(path, options.determineAllPermissions(), options.determineAllRestrictions(), allow);
        }
        context.logResult(this, allow ? "allow {} {} {}" : "deny {} {} {}", result, path, permissions);
        return result;
    }

    private AclResult apply(String path, List<String> permissions, Map<String, Object> restrictions, boolean allow) {
        AclResult result;
        if (context.getPermissionsManager().check(authorizable, path, permissions, restrictions, allow)) {
            result = AclResult.OK;
        } else {
            context.getPermissionsManager().apply(authorizable, path, permissions, restrictions, allow);
            result = AclResult.CHANGED;
        }
        return result;
    }

    public AclResult allow(com.wttech.aem.contentor.core.acl.authorizable.PermissionsOptions options) {
        return apply(
                options.getPath(),
                options.getPermissions(),
                options.getGlob(),
                options.getTypes(),
                options.getProperties(),
                options.getRestrictions(),
                options.getMode(),
                true);
    }

    public AclResult allow(
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            com.wttech.aem.contentor.core.acl.authorizable.PermissionsOptions.Mode mode) {
        return apply(path, permissions, glob, types, properties, restrictions, mode, true);
    }

    public AclResult allow(String path, List<String> permissions) {
        return apply(path, permissions, null, null, null, null, null, true);
    }

    public AclResult allow(String path, List<String> permissions, String glob) {
        return apply(path, permissions, glob, null, null, null, null, true);
    }

    public AclResult allow(String path, List<String> permissions, Map<String, Object> restrictions) {
        return apply(path, permissions, null, null, null, restrictions, null, true);
    }

    public AclResult deny(com.wttech.aem.contentor.core.acl.authorizable.PermissionsOptions options) {
        return apply(
                options.getPath(),
                options.getPermissions(),
                options.getGlob(),
                options.getTypes(),
                options.getProperties(),
                options.getRestrictions(),
                options.getMode(),
                false);
    }

    public AclResult deny(
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            PermissionsOptions.Mode mode) {
        return apply(path, permissions, glob, types, properties, restrictions, mode, false);
    }

    public AclResult deny(String path, List<String> permissions) {
        return apply(path, permissions, null, null, null, null, null, false);
    }

    public AclResult deny(String path, List<String> permissions, String glob) {
        return apply(path, permissions, glob, null, null, null, null, false);
    }

    public AclResult deny(String path, List<String> permissions, Map<String, Object> restrictions) {
        return apply(path, permissions, null, null, null, restrictions, null, false);
    }

    public AclResult setProperty(SetPropertyOptions options) {
        return setProperty(options.getRelPath(), options.getValue());
    }

    public AclResult setProperty(String relPath, String value) {
        AclResult result;
        if (authorizable == null) {
            result = AclResult.SKIPPED;
        } else {
            List<String> values = context.getAuthorizableManager().getProperty(authorizable, relPath);
            if (values != null && values.contains(value)) {
                result = AclResult.OK;
            } else {
                context.getAuthorizableManager().setProperty(authorizable, relPath, value);
                result = AclResult.CHANGED;
            }
        }
        context.logResult(this, "setProperty {} {} {}", result, relPath, value);
        return result;
    }

    public AclResult removeProperty(RemovePropertyOptions options) {
        return removeProperty(options.getRelPath());
    }

    public AclResult removeProperty(String relPath) {
        AclResult result;
        if (authorizable == null) {
            result = AclResult.SKIPPED;
        } else {
            result = context.getAuthorizableManager().removeProperty(authorizable, relPath)
                    ? AclResult.CHANGED
                    : AclResult.OK;
        }
        context.logResult(this, "removeProperty {} {}", result, relPath);
        return result;
    }

    public Authorizable get() {
        return authorizable;
    }

    public String getId() {
        return id;
    }
}
