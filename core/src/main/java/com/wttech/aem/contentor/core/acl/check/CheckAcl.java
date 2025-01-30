package com.wttech.aem.contentor.core.acl.check;

import com.wttech.aem.contentor.core.acl.AclException;
import com.wttech.aem.contentor.core.acl.utils.AuthorizableManager;
import com.wttech.aem.contentor.core.acl.utils.PermissionsManager;
import com.wttech.aem.contentor.core.util.GroovyUtils;
import groovy.lang.Closure;
import java.util.List;
import java.util.Map;
import javax.jcr.RepositoryException;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;

public class CheckAcl {

    private final JackrabbitSession session;

    private final AuthorizableManager authorizableManager;

    private final PermissionsManager permissionsManager;

    public CheckAcl(
            JackrabbitSession session, AuthorizableManager authorizableManager, PermissionsManager permissionsManager) {
        this.session = session;
        this.authorizableManager = authorizableManager;
        this.permissionsManager = permissionsManager;
    }

    // TODO Closure accepting methods need to be defined before the simple ones (add arch unit rule to protect it)
    public boolean exclude(Closure<ExcludeOptions> closure) {
        return exclude(GroovyUtils.with(new ExcludeOptions(), closure));
    }

    public boolean exists(Closure<ExistsOptions> closure) {
        return exists(GroovyUtils.with(new ExistsOptions(), closure));
    }

    public boolean include(Closure<IncludeOptions> closure) {
        return include(GroovyUtils.with(new IncludeOptions(), closure));
    }

    public boolean notExists(Closure<NotExistsOptions> closure) {
        return notExists(GroovyUtils.with(new NotExistsOptions(), closure));
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

    // Non-closure accepting methods

    public boolean exclude(ExcludeOptions options) {
        return exclude(options.getGroupId(), options.getId());
    }

    public boolean exclude(String groupId, String id) {
        try {
            Group group = authorizableManager.getGroup(groupId);
            Authorizable authorizable = authorizableManager.getAuthorizable(id);
            if (authorizable != null) {
                return !group.isMember(authorizable);
            }
            return true;
        } catch (RepositoryException e) {
            throw new AclException("Failed to check if group does not contain authorizable", e);
        }
    }

    public boolean exists(ExistsOptions options) {
        return exists(options.getId(), options.getPath(), options.getType());
    }

    public boolean exists(String id, String path, ExistsOptions.Type type) {
        try {
            Authorizable authorizable = authorizableManager.getAuthorizable(id);
            boolean result = authorizable != null;
            if (StringUtils.isNotEmpty(path)) {
                result &= authorizable != null && StringUtils.endsWith(authorizable.getPath(), path);
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
            }
            return result;
        } catch (RepositoryException e) {
            throw new AclException("Failed to check if authorizable exists", e);
        }
    }

    public boolean exists(String id) {
        return exists(id, null, ExistsOptions.Type.AUTHORIZABLE);
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

    public boolean include(IncludeOptions options) {
        return include(options.getGroupId(), options.getId(), options.isIfExists());
    }

    public boolean include(String groupId, String id, boolean ifExists) {
        try {
            Group group = authorizableManager.getGroup(groupId);
            Authorizable authorizable = authorizableManager.getAuthorizable(id);
            if (authorizable == null) {
                return ifExists;
            } else {
                return group.isMember(authorizable);
            }
        } catch (RepositoryException e) {
            throw new AclException("Failed to check if group contains authorizable", e);
        }
    }

    public boolean notExists(NotExistsOptions options) {
        return notExists(options.getId());
    }

    public boolean notExists(String id) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        return authorizable == null;
    }

    public boolean password(PasswordOptions options) {
        return password(options.getId(), options.getPassword());
    }

    public boolean password(String id, String password) {
        User user = authorizableManager.getUser(id);
        return authorizableManager.testPassword(user, password);
    }

    public boolean allow(PermissionsOptions options) {
        Authorizable authorizable = authorizableManager.getAuthorizable(options.getId());
        return permissionsManager.check(
                authorizable,
                options.getPath(),
                options.determineAllPermissions(),
                options.determineAllRestrictions(),
                true);
    }

    public boolean allow(String id, String path, List<String> permissions) {
        PermissionsOptions options = new PermissionsOptions();
        options.setId(id);
        options.setPath(path);
        options.setPermissions(permissions);
        return allow(options);
    }

    public boolean allow(String id, String path, List<String> permissions, Map<String, Object> restrictions) {
        PermissionsOptions options = new PermissionsOptions();
        options.setId(id);
        options.setPath(path);
        options.setPermissions(permissions);
        options.setRestrictions(restrictions);
        return allow(options);
    }

    public boolean deny(PermissionsOptions options) {
        Authorizable authorizable = authorizableManager.getAuthorizable(options.getId());
        return permissionsManager.check(
                authorizable,
                options.getPath(),
                options.determineAllPermissions(),
                options.determineAllRestrictions(),
                false);
    }

    public boolean deny(String id, String path, List<String> permissions) {
        PermissionsOptions options = new PermissionsOptions();
        options.setId(id);
        options.setPath(path);
        options.setPermissions(permissions);
        return deny(options);
    }

    public boolean deny(String id, String path, List<String> permissions, Map<String, Object> restrictions) {
        PermissionsOptions options = new PermissionsOptions();
        options.setId(id);
        options.setPath(path);
        options.setPermissions(permissions);
        options.setRestrictions(restrictions);
        return deny(options);
    }

    public boolean property(PropertyOptions options) {
        return property(options.getId(), options.getName(), options.getValue());
    }

    public boolean property(String id, String relPath, String value) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        List<String> values = authorizableManager.getProperty(authorizable, relPath);
        return values != null && values.contains(value);
    }
}
