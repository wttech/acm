package com.wttech.aem.contentor.core.acl.authorizable;

import com.wttech.aem.contentor.core.acl.AclException;
import com.wttech.aem.contentor.core.acl.AclResult;
import com.wttech.aem.contentor.core.acl.utils.AuthorizableManager;
import com.wttech.aem.contentor.core.acl.utils.PathUtils;
import com.wttech.aem.contentor.core.acl.utils.PermissionsManager;
import com.wttech.aem.contentor.core.util.GroovyUtils;
import groovy.lang.Closure;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.jcr.RepositoryException;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.sling.api.resource.ResourceResolver;

public class MyAuthorizable {

    private static final String EVERYONE = "everyone";

    protected final Authorizable authorizable;

    protected final ResourceResolver resourceResolver;

    protected final AuthorizableManager authorizableManager;

    protected final PermissionsManager permissionsManager;

    protected final boolean compositeNodeStore;

    public MyAuthorizable(
            Authorizable authorizable,
            ResourceResolver resourceResolver,
            AuthorizableManager authorizableManager,
            PermissionsManager permissionsManager,
            boolean compositeNodeStore) {
        this.authorizable = authorizable;
        this.resourceResolver = resourceResolver;
        this.authorizableManager = authorizableManager;
        this.permissionsManager = permissionsManager;
        this.compositeNodeStore = compositeNodeStore;
    }

    // TODO Closure accepting methods need to be defined before the simple ones (add arch unit rule to protect it)
    public AclResult addToGroup(Closure<GroupOptions> closure) {
        return addToGroup(GroovyUtils.with(new GroupOptions(), closure));
    }

    public AclResult removeFromGroup(Closure<GroupOptions> closure) {
        return removeFromGroup(GroovyUtils.with(new GroupOptions(), closure));
    }

    public AclResult removeFromAllGroups(Closure<EmptyOptions> closure) {
        return removeFromAllGroups();
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

    // Non-closure accepting methods

    public AclResult addToGroup(GroupOptions options) {
        Authorizable group = determineAuthorizable(options.getGroup(), options.getGroupId());
        return addToGroup(group);
    }

    public AclResult addToGroup(Authorizable group) {
        AclResult result;
        if (notExists(authorizable)) {
            result = AclResult.SKIPPED;
        } else if (notExists(group)) {
            result = AclResult.SKIPPED;
        } else if (!group.isGroup()) {
            result = AclResult.SKIPPED;
        } else {
            result = authorizableManager.addMember((Group) group, authorizable)
                    ? AclResult.DONE
                    : AclResult.ALREADY_DONE;
        }
        return result;
    }

    public AclResult addToGroup(String groupId) {
        Authorizable group = determineAuthorizable(groupId);
        return addToGroup(group);
    }

    public AclResult removeFromGroup(GroupOptions options) {
        Authorizable group = determineAuthorizable(options.getGroup(), options.getGroupId());
        return removeFromGroup(group);
    }

    public AclResult removeFromGroup(Authorizable group) {
        AclResult result;
        if (notExists(authorizable)) {
            result = AclResult.SKIPPED;
        } else if (notExists(group)) {
            result = AclResult.SKIPPED;
        } else if (!group.isGroup()) {
            result = AclResult.SKIPPED;
        } else {
            result = authorizableManager.removeMember((Group) group, authorizable)
                    ? AclResult.DONE
                    : AclResult.ALREADY_DONE;
        }
        return result;
    }

    public AclResult removeFromGroup(String groupId) {
        Authorizable group = determineAuthorizable(groupId);
        return removeFromGroup(group);
    }

    public AclResult removeFromAllGroups() {
        try {
            AclResult result;
            if (notExists(authorizable)) {
                result = AclResult.SKIPPED;
            } else {
                Iterator<Group> groups = authorizable.memberOf();
                result = AclResult.ALREADY_DONE;
                while (groups.hasNext()) {
                    Group group = groups.next();
                    if (!StringUtils.equals(group.getID(), EVERYONE)
                            && authorizableManager.removeMember(group, authorizable)) {
                        result = AclResult.DONE;
                    }
                }
            }
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
        if (notExists(authorizable)) {
            result = AclResult.SKIPPED;
        } else if (compositeNodeStore && PathUtils.isAppsOrLibsPath(path)) {
            result = AclResult.SKIPPED;
        } else if (resourceResolver.getResource(path) == null) {
            result = AclResult.SKIPPED;
        } else {
            result = permissionsManager.clear(authorizable, path, strict) ? AclResult.DONE : AclResult.ALREADY_DONE;
        }
        return result;
    }

    public AclResult clear(String path) {
        return clear(path, false);
    }

    private AclResult apply(
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            PermissionsOptions.Mode mode,
            boolean allow) {
        PermissionsOptions options = new PermissionsOptions();
        options.setPermissions(permissions);
        options.setGlob(glob);
        options.setTypes(types);
        options.setProperties(properties);
        options.setRestrictions(restrictions);
        return apply(path, options.determineAllPermissions(), options.determineAllRestrictions(), mode, allow);
    }

    private AclResult apply(
            String path,
            List<String> permissions,
            Map<String, Object> restrictions,
            PermissionsOptions.Mode mode,
            boolean allow) {
        AclResult result;
        if (notExists(authorizable)) {
            result = AclResult.SKIPPED;
        } else if (compositeNodeStore && PathUtils.isAppsOrLibsPath(path)) {
            result = AclResult.SKIPPED;
        } else if (resourceResolver.getResource(path) == null) {
            if (mode == PermissionsOptions.Mode.FAIL) {
                throw new AclException(String.format("Path %s not found", path));
            }
            result = AclResult.SKIPPED;
        } else {
            if (permissionsManager.check(authorizable, path, permissions, restrictions, allow)) {
                result = AclResult.ALREADY_DONE;
            } else {
                permissionsManager.apply(authorizable, path, permissions, restrictions, allow);
                result = AclResult.DONE;
            }
        }
        return result;
    }

    public AclResult allow(
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            PermissionsOptions.Mode mode) {
        return apply(path, permissions, glob, types, properties, restrictions, mode, true);
    }

    public AclResult allow(PermissionsOptions options) {
        return apply(
                options.getPath(),
                options.determineAllPermissions(),
                options.determineAllRestrictions(),
                options.getMode(),
                true);
    }

    public AclResult allow(String path, List<String> permissions) {
        return apply(path, permissions, Collections.emptyMap(), PermissionsOptions.Mode.SKIP, true);
    }

    public AclResult allow(String path, List<String> permissions, Map<String, Object> restrictions) {
        return apply(path, permissions, restrictions, PermissionsOptions.Mode.SKIP, true);
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

    public AclResult deny(PermissionsOptions options) {
        return apply(
                options.getPath(),
                options.determineAllPermissions(),
                options.determineAllRestrictions(),
                options.getMode(),
                false);
    }

    public AclResult deny(String path, List<String> permissions) {
        return apply(path, permissions, Collections.emptyMap(), PermissionsOptions.Mode.SKIP, false);
    }

    public AclResult deny(String path, List<String> permissions, Map<String, Object> restrictions) {
        return apply(path, permissions, restrictions, PermissionsOptions.Mode.SKIP, false);
    }

    public AclResult setProperty(SetPropertyOptions options) {
        return setProperty(options.getRelPath(), options.getValue());
    }

    public AclResult setProperty(String relPath, String value) {
        AclResult result;
        if (notExists(authorizable)) {
            result = AclResult.SKIPPED;
        } else {
            List<String> values = authorizableManager.getProperty(authorizable, relPath);
            if (values != null && values.contains(value)) {
                result = AclResult.ALREADY_DONE;
            } else {
                authorizableManager.setProperty(authorizable, relPath, value);
                result = AclResult.DONE;
            }
        }
        return result;
    }

    public AclResult removeProperty(RemovePropertyOptions options) {
        return removeProperty(options.getRelPath());
    }

    public AclResult removeProperty(String relPath) {
        AclResult result;
        if (notExists(authorizable)) {
            result = AclResult.SKIPPED;
        } else {
            result =
                    authorizableManager.removeProperty(authorizable, relPath) ? AclResult.DONE : AclResult.ALREADY_DONE;
        }
        return result;
    }

    protected Authorizable determineAuthorizable(String id) {
        return determineAuthorizable(null, id);
    }

    protected Authorizable determineAuthorizable(Authorizable authorizable, String id) {
        if (authorizable == null && StringUtils.isNotEmpty(id)) {
            authorizable = authorizableManager.getAuthorizable(id);
        }
        if (authorizable == null) {
            authorizable = new UnknownAuthorizable(StringUtils.defaultString(id));
        }
        return authorizable;
    }

    protected boolean notExists(Authorizable authorizable) {
        return authorizable == null || authorizable instanceof UnknownAuthorizable;
    }

    protected String getID(Authorizable authorizable) {
        try {
            return authorizable.getID();
        } catch (RepositoryException e) {
            return "";
        }
    }
}
