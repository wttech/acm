package com.wttech.aem.contentor.core.checkacl;

import com.wttech.aem.contentor.core.acl.AclException;
import com.wttech.aem.contentor.core.acl.utils.AuthorizableManager;
import com.wttech.aem.contentor.core.checkacl.utils.PermissionManager;
import com.wttech.aem.contentor.core.util.GroovyUtils;
import groovy.lang.Closure;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.ResourceResolver;

public class CheckAcl {

    private final JackrabbitSession session;

    private final AuthorizableManager authorizableManager;

    private final PermissionManager permissionManager;

    public CheckAcl(ResourceResolver resourceResolver) {
        try {
            this.session = (JackrabbitSession) resourceResolver.adaptTo(Session.class);
            UserManager userManager = session.getUserManager();
            ValueFactory valueFactory = session.getValueFactory();
            this.authorizableManager = new AuthorizableManager(userManager, valueFactory);
            this.permissionManager = new PermissionManager(session);
        } catch (RepositoryException e) {
            throw new AclException("Failed to initialize check acl", e);
        }
    }

    // TODO Closure accepting methods need to be defined before the simple ones (add arch unit rule to protect it)
    public boolean exclude(Closure<ExcludeOptions> closure) throws RepositoryException {
        return exclude(GroovyUtils.with(new ExcludeOptions(), closure));
    }

    public boolean exists(Closure<ExistsOptions> closure) throws RepositoryException {
        return exists(GroovyUtils.with(new ExistsOptions(), closure));
    }

    public boolean include(Closure<IncludeOptions> closure) throws RepositoryException {
        return include(GroovyUtils.with(new IncludeOptions(), closure));
    }

    public boolean notExists(Closure<NotExistsOptions> closure) throws RepositoryException {
        return notExists(GroovyUtils.with(new NotExistsOptions(), closure));
    }

    public boolean password(Closure<PasswordOptions> closure) throws RepositoryException {
        return password(GroovyUtils.with(new PasswordOptions(), closure));
    }

    public boolean allow(Closure<AllowOptions> closure) throws RepositoryException {
        return allow(GroovyUtils.with(new AllowOptions(), closure));
    }

    public boolean deny(Closure<DenyOptions> closure) throws RepositoryException {
        return deny(GroovyUtils.with(new DenyOptions(), closure));
    }

    public boolean property(Closure<PropertyOptions> closure) throws RepositoryException {
        return property(GroovyUtils.with(new PropertyOptions(), closure));
    }

    // Non-closure accepting methods

    public boolean exclude(ExcludeOptions options) throws RepositoryException {
        List<String> ids = new ArrayList<>();
        if (options.getIds() != null) {
            ids.addAll(options.getIds());
        }
        if (options.getId() != null) {
            ids.add(options.getId());
        }
        return exclude(options.getGroup(), ids);
    }

    public boolean exclude(String group, String id) throws RepositoryException {
        return exclude(group, Collections.singletonList(id));
    }

    public boolean exclude(String group, List<String> ids) throws RepositoryException {
        boolean result = true;
        Group groupAuthorizable = authorizableManager.getGroup(group);
        for (String id : ids) {
            Authorizable authorizable = authorizableManager.getAuthorizable(id);
            if (authorizable != null) {
                result &= !groupAuthorizable.isMember(authorizable);
            }
        }
        return result;
    }

    public boolean exists(ExistsOptions options) throws RepositoryException {
        return exists(options.getId(), options.getPath(), options.getType());
    }

    public boolean exists(String id, String path, ExistsOptions.Type type) throws RepositoryException {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        boolean result = authorizable != null;
        if (StringUtils.isNotEmpty(path)) {
            result &= authorizable != null && StringUtils.endsWith(authorizable.getPath(), path);
        }
        switch (type) {
            case USER:
                result &= authorizable != null && !authorizable.isGroup();
                if (result) {
                    User user = (User) authorizable;
                    result = !user.isSystemUser();
                }
                break;
            case SYSTEM_USER:
                result &= authorizable != null && !authorizable.isGroup();
                if (result) {
                    User user = (User) authorizable;
                    result = user.isSystemUser();
                }
                break;
            case GROUP:
                result &= authorizable != null && authorizable.isGroup();
                break;
        }
        return result;
    }

    public boolean exists(String id) throws RepositoryException {
        return exists(id, null, ExistsOptions.Type.AUTHORIZABLE);
    }

    public boolean existsUser(String id) throws RepositoryException {
        return exists(id, null, ExistsOptions.Type.USER);
    }

    public boolean existsSystemUser(String id) throws RepositoryException {
        return exists(id, null, ExistsOptions.Type.SYSTEM_USER);
    }

    public boolean existsGroup(String id) throws RepositoryException {
        return exists(id, null, ExistsOptions.Type.GROUP);
    }

    public boolean include(IncludeOptions options) throws RepositoryException {
        List<String> ids = new ArrayList<>();
        if (options.getIds() != null) {
            ids.addAll(options.getIds());
        }
        if (options.getId() != null) {
            ids.add(options.getId());
        }
        return include(options.getGroup(), ids, options.isIfExists());
    }

    public boolean include(String group, String id, boolean ifExists) throws RepositoryException {
        return include(group, Collections.singletonList(id), ifExists);
    }

    public boolean include(String group, List<String> ids, boolean ifExists) throws RepositoryException {
        boolean result = true;
        Group groupAuthorizable = authorizableManager.getGroup(group);
        for (String id : ids) {
            Authorizable authorizable = authorizableManager.getAuthorizable(id);
            if (authorizable == null) {
                result &= ifExists;
            } else {
                result &= groupAuthorizable.isMember(authorizable);
            }
        }
        return result;
    }

    public boolean notExists(NotExistsOptions options) throws RepositoryException {
        List<String> ids = new ArrayList<>();
        if (options.getIds() != null) {
            ids.addAll(options.getIds());
        }
        if (options.getId() != null) {
            ids.add(options.getId());
        }
        return notExists(ids);
    }

    public boolean notExists(String id) throws RepositoryException {
        return notExists(Collections.singletonList(id));
    }

    public boolean notExists(List<String> ids) throws RepositoryException {
        boolean result = true;
        for (String id : ids) {
            Authorizable authorizable = authorizableManager.getAuthorizable(id);
            result &= authorizable == null;
        }
        return result;
    }

    public boolean password(PasswordOptions options) throws RepositoryException {
        return password(options.getId(), options.getPassword());
    }

    public boolean password(String id, String password) throws RepositoryException {
        Repository repository = session.getRepository();
        Credentials credentials = new SimpleCredentials(id, password.toCharArray());
        try {
            repository.login(credentials).logout();
        } catch (LoginException e) {
            return false;
        }
        return true;
    }

    public boolean allow(AllowOptions options) throws RepositoryException {
        Authorizable authorizable = authorizableManager.getAuthorizable(options.getId());
        return permissionManager.checkPermissions(
                authorizable, options.getPath(), options.getPermissions(), options.getGlob(), true);
    }

    public boolean allow(String id, String path, List<String> permissions) throws RepositoryException {
        AllowOptions options = new AllowOptions();
        options.setId(id);
        options.setPath(path);
        options.setPermissions(permissions);
        return allow(options);
    }

    public boolean deny(DenyOptions options) throws RepositoryException {
        Authorizable authorizable = authorizableManager.getAuthorizable(options.getId());
        return permissionManager.checkPermissions(
                authorizable, options.getPath(), options.getPermissions(), options.getGlob(), false);
    }

    public boolean deny(String id, String path, List<String> permissions) throws RepositoryException {
        DenyOptions options = new DenyOptions();
        options.setId(id);
        options.setPath(path);
        options.setPermissions(permissions);
        return deny(options);
    }

    public boolean property(PropertyOptions options) throws RepositoryException {
        return property(options.getId(), options.getName(), options.getValue());
    }

    public boolean property(String id, String name, String value) throws RepositoryException {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        Value[] values = authorizable.getProperty(name);
        if (values == null) {
            return false;
        }
        for (Value val : values) {
            if (StringUtils.equals(val.getString(), value)) {
                return true;
            }
        }
        return false;
    }
}
