package com.wttech.aem.contentor.core.acl.authorizable;

import com.wttech.aem.contentor.core.acl.AclContext;
import com.wttech.aem.contentor.core.acl.AclException;
import com.wttech.aem.contentor.core.acl.AclResult;
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

    public AclResult addToGroup(Closure<GroupOptions> closure) {
        return addToGroup(GroovyUtils.with(new GroupOptions(), closure));
    }

    public AclResult removeFromGroup(Closure<GroupOptions> closure) {
        return removeFromGroup(GroovyUtils.with(new GroupOptions(), closure));
    }

    public AclResult clear(Closure<ClearOptions> closure) {
        return clear(GroovyUtils.with(new ClearOptions(), closure));
    }

    public AclResult allow(Closure<PermissionsOptions> closure) {
        return allow(GroovyUtils.with(new PermissionsOptions(), closure));
    }

    public AclResult deny(Closure<PermissionsOptions> closure) {
        return deny(GroovyUtils.with(new PermissionsOptions(), closure));
    }

    public AclResult setProperty(Closure<SetPropertyOptions> closure) {
        return setProperty(GroovyUtils.with(new SetPropertyOptions(), closure));
    }

    public AclResult removeProperty(Closure<RemovePropertyOptions> closure) {
        return removeProperty(GroovyUtils.with(new RemovePropertyOptions(), closure));
    }

    public AclResult addToGroup(GroupOptions options) {
        AclGroup group = context.determineGroup(options.getGroup(), options.getGroupId());
        String groupId = context.determineId(options.getGroup(), options.getGroupId());
        AclResult result;
        if (group == null) {
            result = AclResult.SKIPPED;
        } else {
            result = context.getAuthorizableManager().addMember(group.get(), authorizable)
                    ? AclResult.CHANGED
                    : AclResult.OK;
        }
        context.getLogger().info("Added authorizable '{}' to group '{}' [{}]", id, groupId, result);
        return result;
    }

    public AclResult addToGroup(String groupId) {
        return addToGroup(GroupOptions.of(null, groupId));
    }

    public AclResult addToGroup(AclGroup group) {
        return addToGroup(GroupOptions.of(group, null));
    }

    public AclResult removeFromGroup(GroupOptions options) {
        AclGroup group = context.determineGroup(options.getGroup(), options.getGroupId());
        String groupId = context.determineId(options.getGroup(), options.getGroupId());
        AclResult result;
        if (group == null) {
            result = AclResult.SKIPPED;
        } else {
            result = context.getAuthorizableManager().removeMember(group.get(), authorizable)
                    ? AclResult.CHANGED
                    : AclResult.OK;
        }
        context.getLogger().info("Removed authorizable '{}' from group '{}' [{}]", id, groupId, result);
        return result;
    }

    public AclResult removeFromGroup(String groupId) {
        return removeFromGroup(GroupOptions.of(null, groupId));
    }

    public AclResult removeFromGroup(AclGroup group) {
        return removeFromGroup(GroupOptions.of(group, null));
    }

    public AclResult removeFromAllGroups() {
        try {
            Iterator<Group> groups = authorizable.memberOf();
            AclResult result = AclResult.OK;
            while (groups.hasNext()) {
                Group group = groups.next();
                if (!StringUtils.equals(group.getID(), EVERYONE)
                        && context.getAuthorizableManager().removeMember(group, authorizable)) {
                    result = AclResult.CHANGED;
                }
            }
            context.getLogger().info("Removed authorizable '{}' from all groups [{}]", id, result);
            return result;
        } catch (RepositoryException e) {
            throw new AclException(String.format("Failed to remove authorizable '%s' from all groups", id), e);
        }
    }

    public AclResult clear(ClearOptions options) {
        return clear(options.getPath(), options.isStrict());
    }

    public AclResult clear(String path, boolean strict) {
        AclResult result;
        if (context.isCompositeNodeStore() && PathUtils.isAppsOrLibsPath(path)) {
            result = AclResult.SKIPPED;
        } else if (context.getResourceResolver().getResource(path) == null) {
            result = AclResult.SKIPPED;
        } else {
            result = context.getPermissionsManager().clear(authorizable, path, strict)
                    ? AclResult.CHANGED
                    : AclResult.OK;
        }
        context.getLogger().info("Cleared permissions for authorizable '{}' at path '{}' [{}]", id, path, result);
        return result;
    }

    public AclResult clear(String path) {
        return clear(path, false);
    }

    public AclResult purge() {
        return AclResult.SKIPPED;
    }

    private AclResult apply(PermissionsOptions options, boolean allow) {
        String path = options.getPath();
        List<String> permissions = options.determineAllPermissions();
        Map<String, Object> restrictions = options.determineAllRestrictions();
        PermissionsOptions.Mode mode = options.getMode();
        AclResult result;
        if (context.isCompositeNodeStore() && PathUtils.isAppsOrLibsPath(path)) {
            result = AclResult.SKIPPED;
        } else if (context.getResourceResolver().getResource(path) == null) {
            if (mode == PermissionsOptions.Mode.FAIL) {
                throw new AclException(String.format("Path '%s' not found", path));
            }
            result = AclResult.SKIPPED;
        } else if (context.getPermissionsManager().check(authorizable, path, permissions, restrictions, allow)) {
            result = AclResult.OK;
        } else {
            context.getPermissionsManager().apply(authorizable, path, permissions, restrictions, allow);
            result = AclResult.CHANGED;
        }
        context.getLogger()
                .info(
                        "Applied {} permissions for authorizable '{}' at path '{}' [{}]",
                        allow ? "allow" : "deny",
                        id,
                        options.getPath(),
                        result);
        return result;
    }

    public AclResult allow(PermissionsOptions options) {
        return apply(options, true);
    }

    public AclResult allow(
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            PermissionsOptions.Mode mode) {
        return allow(PermissionsOptions.of(path, permissions, glob, types, properties, restrictions, mode));
    }

    public AclResult allow(String path, List<String> permissions) {
        return allow(PermissionsOptions.of(path, permissions, null, null));
    }

    public AclResult allow(String path, List<String> permissions, String glob) {
        return allow(PermissionsOptions.of(path, permissions, glob, null));
    }

    public AclResult allow(String path, List<String> permissions, Map<String, Object> restrictions) {
        return allow(PermissionsOptions.of(path, permissions, null, restrictions));
    }

    public AclResult deny(PermissionsOptions options) {
        return apply(options, false);
    }

    public AclResult deny(
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            PermissionsOptions.Mode mode) {
        return deny(PermissionsOptions.of(path, permissions, glob, types, properties, restrictions, mode));
    }

    public AclResult deny(String path, List<String> permissions) {
        return deny(PermissionsOptions.of(path, permissions, null, null));
    }

    public AclResult deny(String path, List<String> permissions, String glob) {
        return deny(PermissionsOptions.of(path, permissions, glob, null));
    }

    public AclResult deny(String path, List<String> permissions, Map<String, Object> restrictions) {
        return deny(PermissionsOptions.of(path, permissions, null, restrictions));
    }

    public AclResult setProperty(SetPropertyOptions options) {
        return setProperty(options.getRelPath(), options.getValue());
    }

    public AclResult setProperty(String relPath, String value) {
        AclResult result;
        List<String> values = context.getAuthorizableManager().getProperty(authorizable, relPath);
        if (values != null && values.contains(value)) {
            result = AclResult.OK;
        } else {
            context.getAuthorizableManager().setProperty(authorizable, relPath, value);
            result = AclResult.CHANGED;
        }
        context.getLogger().info("Set property '{}' for authorizable '{}' [{}]", relPath, id, result);
        return result;
    }

    public AclResult removeProperty(RemovePropertyOptions options) {
        return removeProperty(options.getRelPath());
    }

    public AclResult removeProperty(String relPath) {
        AclResult result = context.getAuthorizableManager().removeProperty(authorizable, relPath)
                ? AclResult.CHANGED
                : AclResult.OK;
        context.getLogger().info("Removed property '{}' for authorizable '{}' [{}]", relPath, id, result);
        return result;
    }

    public Authorizable get() {
        return authorizable;
    }

    public String getId() {
        return id;
    }
}
