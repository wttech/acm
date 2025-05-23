package dev.vml.es.acm.core.acl;

import dev.vml.es.acm.core.acl.check.AuthorizableOptions;
import dev.vml.es.acm.core.acl.check.ExistsOptions;
import dev.vml.es.acm.core.acl.check.MemberOptions;
import dev.vml.es.acm.core.acl.check.PasswordOptions;
import dev.vml.es.acm.core.acl.check.PermissionsOptions;
import dev.vml.es.acm.core.acl.check.PropertyOptions;
import dev.vml.es.acm.core.util.GroovyUtils;
import groovy.lang.Closure;
import java.util.List;
import java.util.Map;
import javax.jcr.RepositoryException;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;

public class AclChecker {

    private final AclContext context;

    public AclChecker(AclContext context) {
        this.context = context;
    }

    // Overloaded methods using Groovy closures
    public boolean exclude(Closure<MemberOptions> closure) {
        return exclude(GroovyUtils.with(new MemberOptions(), closure));
    }

    public boolean exists(Closure<ExistsOptions> closure) {
        return exists(GroovyUtils.with(new ExistsOptions(), closure));
    }

    public boolean include(Closure<MemberOptions> closure) {
        return include(GroovyUtils.with(new MemberOptions(), closure));
    }

    public boolean notExists(Closure<AuthorizableOptions> closure) {
        return notExists(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public boolean password(Closure<PasswordOptions> closure) {
        return password(GroovyUtils.with(new PasswordOptions(), closure));
    }

    public boolean allow(Closure<PermissionsOptions> closure) {
        return allow(GroovyUtils.with(new PermissionsOptions(), closure));
    }

    public boolean deny(Closure<PermissionsOptions> closure) {
        return deny(GroovyUtils.with(new PermissionsOptions(), closure));
    }

    public boolean property(Closure<PropertyOptions> closure) {
        return property(GroovyUtils.with(new PropertyOptions(), closure));
    }

    // Overloaded methods delegating to primary methods
    public boolean exclude(MemberOptions options) {
        return exclude(options.getId(), options.getMemberId());
    }

    public boolean exists(ExistsOptions options) {
        return exists(options.getId(), options.getPath(), options.getType());
    }

    public boolean existsUser(String id) {
        return exists(id, null, ExistsOptions.Type.USER);
    }

    public boolean existsSystemUser(String id) {
        return exists(id, null, ExistsOptions.Type.SYSTEM_USER);
    }

    public boolean existsGroup(String id) {
        return exists(id, null, ExistsOptions.Type.GROUP);
    }

    public boolean include(MemberOptions options) {
        return include(options.getId(), options.getMemberId());
    }

    public boolean notExists(AuthorizableOptions options) {
        return notExists(options.getId());
    }

    public boolean password(PasswordOptions options) {
        return password(options.getId(), options.getPassword());
    }

    public boolean allow(PermissionsOptions options) {
        return check(
                options.getId(),
                options.getPath(),
                options.getPermissions(),
                options.getGlob(),
                options.getTypes(),
                options.getProperties(),
                options.getRestrictions(),
                true);
    }

    public boolean allow(String id, String path, List<String> permissions) {
        return check(id, path, permissions, null, null, null, null, true);
    }

    public boolean allow(String id, String path, List<String> permissions, String glob) {
        return check(id, path, permissions, glob, null, null, null, true);
    }

    public boolean allow(String id, String path, List<String> permissions, Map<String, Object> restrictions) {
        return check(id, path, permissions, null, null, null, restrictions, true);
    }

    public boolean deny(PermissionsOptions options) {
        return check(
                options.getId(),
                options.getPath(),
                options.getPermissions(),
                options.getGlob(),
                options.getTypes(),
                options.getProperties(),
                options.getRestrictions(),
                false);
    }

    public boolean deny(String id, String path, List<String> permissions) {
        return check(id, path, permissions, null, null, null, null, false);
    }

    public boolean deny(String id, String path, List<String> permissions, String glob) {
        return check(id, path, permissions, glob, null, null, null, false);
    }

    public boolean deny(String id, String path, List<String> permissions, Map<String, Object> restrictions) {
        return check(id, path, permissions, null, null, null, restrictions, false);
    }

    public boolean property(PropertyOptions options) {
        return property(options.getId(), options.getName(), options.getValue());
    }

    // Primary methods with logging
    public boolean exclude(String groupId, String memberId) {
        try {
            Group group = context.getAuthorizableManager().getGroup(groupId);
            Authorizable authorizable = context.getAuthorizableManager().getAuthorizable(memberId);
            boolean result = authorizable == null || !group.isMember(authorizable);
            context.getLogger().info("Checked if group '{}' excludes member '{}' [{}]", groupId, memberId, result);
            return result;
        } catch (RepositoryException e) {
            throw new AclException(
                    String.format("Cannot check if group '%s' does not contain authorizable '%s'!", groupId, memberId),
                    e);
        }
    }

    public boolean exists(String id, String path, ExistsOptions.Type type) {
        try {
            Authorizable authorizable = context.getAuthorizableManager().getAuthorizable(id);
            boolean result = authorizable != null;
            if (result && StringUtils.isNotEmpty(path)) {
                result = StringUtils.endsWith(authorizable.getPath(), path);
            }
            switch (type) {
                case USER:
                    result &= authorizable instanceof User;
                    if (result) {
                        User user = (User) authorizable;
                        result = !user.isSystemUser();
                    }
                    break;
                case SYSTEM_USER:
                    result &= authorizable instanceof User;
                    if (result) {
                        User user = (User) authorizable;
                        result = user.isSystemUser();
                    }
                    break;
                case GROUP:
                    result &= authorizable instanceof Group;
                    break;
                default:
                    result = false;
                    break;
            }
            context.getLogger().info("Checked existence for '{}' with type '{}' [{}]", id, type, result);
            return result;
        } catch (RepositoryException e) {
            throw new AclException(String.format("Cannot check if authorizable '%s' exists", id), e);
        }
    }

    public boolean include(String groupId, String memberId) {
        try {
            Group group = context.getAuthorizableManager().getGroup(groupId);
            Authorizable authorizable = context.getAuthorizableManager().getAuthorizable(memberId);
            boolean result = authorizable != null && group.isMember(authorizable);
            context.getLogger().info("Checked if group '{}' includes member '{}' [{}]", groupId, memberId, result);
            return result;
        } catch (RepositoryException e) {
            throw new AclException(
                    String.format("Cannot check if group '%s' contains authorizable '%s'!", groupId, memberId), e);
        }
    }

    public boolean notExists(String id) {
        boolean result = context.getAuthorizableManager().getAuthorizable(id) == null;
        context.getLogger().info("Checked non-existence for authorizable '{}' [{}]", id, result);
        return result;
    }

    public boolean password(String id, String password) {
        User user = context.getAuthorizableManager().getUser(id);
        boolean result = context.getAuthorizableManager().testPassword(user, password);
        context.getLogger().info("Checked password for user '{}' [{}]", id, result);
        return result;
    }

    private boolean check(
            String id,
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            boolean allow) {
        Authorizable authorizable = context.getAuthorizableManager().getAuthorizable(id);
        PermissionsOptions options = new PermissionsOptions();
        options.setPermissions(permissions);
        options.setGlob(glob);
        options.setTypes(types);
        options.setProperties(properties);
        options.setRestrictions(restrictions);
        boolean result = context.getPermissionsManager()
                .check(
                        authorizable,
                        path,
                        options.determineAllPermissions(),
                        options.determineAllRestrictions(),
                        allow);
        context.getLogger()
                .info(
                        "Checked permissions for authorizable '{}' at path '{}' with allow '{}' [{}]",
                        id,
                        path,
                        allow,
                        result);
        return result;
    }

    public boolean property(String id, String relPath, String value) {
        Authorizable authorizable = context.getAuthorizableManager().getAuthorizable(id);
        List<String> values = context.getAuthorizableManager().getProperty(authorizable, relPath);
        boolean result = values != null && values.contains(value);
        context.getLogger().info("Checked property '{}' for authorizable '{}' [{}]", relPath, id, result);
        return result;
    }
}
