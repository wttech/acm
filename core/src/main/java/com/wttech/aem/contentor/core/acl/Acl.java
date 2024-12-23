package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.utils.AuthorizableManager;
import com.wttech.aem.contentor.core.acl.utils.PathUtils;
import com.wttech.aem.contentor.core.acl.utils.PermissionManager;
import com.wttech.aem.contentor.core.acl.utils.PurgeManager;
import com.wttech.aem.contentor.core.acl.utils.RuntimeUtils;
import com.wttech.aem.contentor.core.util.GroovyUtils;
import groovy.lang.Closure;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public class Acl {

    private final ResourceResolver resourceResolver;

    private final JackrabbitSession session;

    private final AuthorizableManager authorizableManager;

    private final PermissionManager permissionManager;

    private final PurgeManager purgeManager;

    private final boolean compositeNodeStore;

    public Acl(ResourceResolver resourceResolver) {
        try {
            this.resourceResolver = resourceResolver;
            this.session = (JackrabbitSession) resourceResolver.adaptTo(Session.class);
            UserManager userManager = session.getUserManager();
            AccessControlManager accessControlManager = session.getAccessControlManager();
            ValueFactory valueFactory = session.getValueFactory();
            this.authorizableManager = new AuthorizableManager(userManager, valueFactory);
            this.permissionManager = new PermissionManager(accessControlManager, valueFactory);
            this.purgeManager = new PurgeManager(session, accessControlManager);
            this.compositeNodeStore = RuntimeUtils.determineCompositeNodeStore(session);
        } catch (RepositoryException e) {
            throw new AclException("Failed to initialize acl", e);
        }
    }

    // TODO Closure accepting methods need to be defined before the simple ones (add arch unit rule to protect it)
    public User createUser(Closure<CreateUserOptions> closure) throws RepositoryException {
        return createUser(GroovyUtils.with(new CreateUserOptions(), closure));
    }

    public Group createGroup(Closure<CreateGroupOptions> closure) throws RepositoryException {
        return createGroup(GroovyUtils.with(new CreateGroupOptions(), closure));
    }

    public void removeUser(Closure<AuthorizableOptions> closure) throws RepositoryException {
        removeUser(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public void removeGroup(Closure<AuthorizableOptions> closure) throws RepositoryException {
        removeGroup(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public void addToGroup(Closure<GroupOptions> closure) throws RepositoryException {
        addToGroup(GroovyUtils.with(new GroupOptions(), closure));
    }

    public void removeFromGroup(Closure<GroupOptions> closure) throws RepositoryException {
        removeFromGroup(GroovyUtils.with(new GroupOptions(), closure));
    }

    public void addMember(Closure<MemberOptions> closure) throws RepositoryException {
        addMember(GroovyUtils.with(new MemberOptions(), closure));
    }

    public void removeMember(Closure<MemberOptions> closure) throws RepositoryException {
        removeMember(GroovyUtils.with(new MemberOptions(), closure));
    }

    public void purge(Closure<PurgeOptions> closure) throws RepositoryException {
        purge(GroovyUtils.with(new PurgeOptions(), closure));
    }

    public AclResult allow(Closure<AllowOptions> closure) throws RepositoryException {
        return allow(GroovyUtils.with(new AllowOptions(), closure));
    }

    public AclResult deny(Closure<DenyOptions> closure) throws RepositoryException {
        return deny(GroovyUtils.with(new DenyOptions(), closure));
    }

    public void setProperty(Closure<PropertyOptions> closure) throws RepositoryException {
        setProperty(GroovyUtils.with(new PropertyOptions(), closure));
    }

    public void setPassword(Closure<PasswordOptions> closure) throws RepositoryException {
        setPassword(GroovyUtils.with(new PasswordOptions(), closure));
    }

    // Non-closure accepting methods

    public User createUser(String id) throws RepositoryException {
        CreateUserOptions options = new CreateUserOptions();
        options.setId(id);
        return createUser(options);
    }

    public User createUser(CreateUserOptions options) throws RepositoryException {
        User user = authorizableManager.getUser(options.getId());
        if (user == null) {
            if (options.isSystemUser()) {
                user = authorizableManager.createSystemUser(options.getId(), options.getPath());
            } else {
                user = authorizableManager.createUser(options.getId(), options.getPassword(), options.getPath());
            }
            authorizableManager.updateUser(user, options.getPassword(), options.determineAllProperties());
        } else if (options.getMode() == CreateAuthorizableOptions.Mode.OVERRIDE) {
            authorizableManager.updateUser(user, options.getPassword(), options.determineAllProperties());
        }
        return user;
    }

    public User getUser(String id) throws RepositoryException {
        return authorizableManager.getUser(id);
    }

    public Group getGroup(String id) throws RepositoryException {
        return authorizableManager.getGroup(id);
    }

    public void removeUser(AuthorizableOptions options) throws RepositoryException {
        User user = (User) determineAuthorizable(options);
        removeUser(user);
    }

    public void removeUser(String id) throws RepositoryException {
        User user = authorizableManager.getUser(id);
        removeUser(user);
    }

    public void removeUser(User user) throws RepositoryException {
        authorizableManager.removeUser(user);
    }

    public User createSystemUser() {
        throw new IllegalStateException("Not implemented yet!");
    }

    public Group createGroup(String id) throws RepositoryException {
        CreateGroupOptions options = new CreateGroupOptions();
        options.setId(id);
        return createGroup(options);
    }

    public Group createGroup(CreateGroupOptions options) throws RepositoryException {
        Group group = authorizableManager.getGroup(options.getId());
        if (group == null) {
            group = authorizableManager.createGroup(options.getId(), options.getPath(), options.getExternalId());
            authorizableManager.updateGroup(group, options.determineAllProperties());
        } else if (options.getMode() == CreateGroupOptions.Mode.OVERRIDE) {
            authorizableManager.updateGroup(group, options.determineAllProperties());
        }
        return group;
    }

    public void removeGroup(AuthorizableOptions options) throws RepositoryException {
        Group group = (Group) determineAuthorizable(options);
        removeGroup(group);
    }

    public void removeGroup(String id) throws RepositoryException {
        Group group = authorizableManager.getGroup(id);
        removeGroup(group);
    }

    public void removeGroup(Group group) throws RepositoryException {
        authorizableManager.removeGroup(group);
    }

    public void addToGroup(GroupOptions options) throws RepositoryException {
        Authorizable authorizable = determineAuthorizable(options);
        Group group = (Group) options.getGroupAuthorizable();
        if (group == null) {
            group = authorizableManager.getGroup(options.getGroupId());
        }
        addToGroup(authorizable, group);
    }

    public void addToGroup(Authorizable authorizable, Group group) throws RepositoryException {
        authorizableManager.addToGroup(authorizable, group);
    }

    public void addToGroup(String id, String groupId) throws RepositoryException {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        Group group = authorizableManager.getGroup(groupId);
        addToGroup(authorizable, group);
    }

    public void removeFromGroup(GroupOptions options) throws RepositoryException {
        Authorizable authorizable = determineAuthorizable(options);
        Group group = (Group) options.getGroupAuthorizable();
        if (group == null) {
            group = authorizableManager.getGroup(options.getGroupId());
        }
        removeFromGroup(authorizable, group);
    }

    public void removeFromGroup(Authorizable authorizable, Group group) throws RepositoryException {
        authorizableManager.removeFromGroup(authorizable, group);
    }

    public void removeFromGroup(String id, String groupId) throws RepositoryException {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        Group group = authorizableManager.getGroup(groupId);
        removeFromGroup(authorizable, group);
    }

    public void addMember(MemberOptions options) throws RepositoryException {
        Group group = (Group) determineAuthorizable(options);
        Authorizable member = options.getMemberAuthorizable();
        if (member == null) {
            member = authorizableManager.getAuthorizable(options.getMemberId());
        }
        addMember(group, member);
    }

    public void addMember(Group group, Authorizable member) throws RepositoryException {
        addToGroup(member, group);
    }

    public void addMember(String id, String memberId) throws RepositoryException {
        Group group = authorizableManager.getGroup(id);
        Authorizable member = authorizableManager.getAuthorizable(memberId);
        addMember(group, member);
    }

    public void removeMember(MemberOptions options) throws RepositoryException {
        Group group = (Group) determineAuthorizable(options);
        Authorizable member = options.getMemberAuthorizable();
        if (member == null) {
            member = authorizableManager.getAuthorizable(options.getMemberId());
        }
        removeMember(group, member);
    }

    public void removeMember(Group group, Authorizable member) throws RepositoryException {
        removeFromGroup(member, group);
    }

    public void removeMember(String id, String memberId) throws RepositoryException {
        Group group = authorizableManager.getGroup(id);
        Authorizable member = authorizableManager.getAuthorizable(memberId);
        removeMember(group, member);
    }

    public void purge(PurgeOptions options) throws RepositoryException {
        Authorizable authorizable = determineAuthorizable(options);
        purge(authorizable, options.getPath(), options.isStrict());
    }

    public AclResult purge(Authorizable authorizable, String path, boolean strict) throws RepositoryException {
        path = StringUtils.defaultString(path, "/");
        if (compositeNodeStore && PathUtils.isAppsOrLibsPath(path)) {
            return AclResult.SKIPPED;
        } else {
            purgeManager.purge(authorizable, path, strict);
            return AclResult.OK;
        }
    }

    public AclResult allow(Authorizable authorizable, String path, List<String> permissions)
            throws RepositoryException {
        AllowOptions options = new AllowOptions();
        options.setAuthorizable(authorizable);
        options.setPath(path);
        options.setPermissions(permissions);
        return allow(options);
    }

    public AclResult allow(String id, String path, List<String> permissions) throws RepositoryException {
        AllowOptions options = new AllowOptions();
        options.setId(id);
        options.setPath(path);
        options.setPermissions(permissions);
        return allow(options);
    }

    public AclResult allow(AllowOptions options) throws RepositoryException {
        Resource resource = resourceResolver.getResource(options.getPath());
        if (resource == null) {
            if (options.getMode() == RestrictionOptions.Mode.FAIL) {
                throw new AclException(String.format("Path %s not found", options.getPath()));
            }
            return AclResult.PATH_NOT_FOUND;
        } else if (compositeNodeStore && PathUtils.isAppsOrLibsPath(options.getPath())) {
            return AclResult.SKIPPED;
        } else {
            Authorizable authorizable = determineAuthorizable(options);
            permissionManager.applyPermissions(
                    authorizable,
                    options.getPath(),
                    options.getPermissions(),
                    options.determineAllRestrictions(),
                    true);
            return AclResult.OK;
        }
    }

    public AclResult deny(Authorizable authorizable, String path, List<String> permissions) throws RepositoryException {
        DenyOptions options = new DenyOptions();
        options.setAuthorizable(authorizable);
        options.setPath(path);
        options.setPermissions(permissions);
        return deny(options);
    }

    public AclResult deny(String id, String path, List<String> permissions) throws RepositoryException {
        DenyOptions options = new DenyOptions();
        options.setId(id);
        options.setPath(path);
        options.setPermissions(permissions);
        return deny(options);
    }

    public AclResult deny(DenyOptions options) throws RepositoryException {
        Resource resource = resourceResolver.getResource(options.getPath());
        if (resource == null) {
            if (options.getMode() == RestrictionOptions.Mode.FAIL) {
                throw new AclException(String.format("Path %s not found", options.getPath()));
            }
            return AclResult.PATH_NOT_FOUND;
        } else if (compositeNodeStore && PathUtils.isAppsOrLibsPath(options.getPath())) {
            return AclResult.SKIPPED;
        } else {
            Authorizable authorizable = determineAuthorizable(options);
            permissionManager.applyPermissions(
                    authorizable,
                    options.getPath(),
                    options.getPermissions(),
                    options.determineAllRestrictions(),
                    false);
            return AclResult.OK;
        }
    }

    public void setProperty(PropertyOptions options) throws RepositoryException {
        Authorizable authorizable = determineAuthorizable(options);
        setProperty(authorizable, options.getKey(), options.getValue());
    }

    public void setProperty(String id, String key, String value) throws RepositoryException {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        setProperty(authorizable, key, value);
    }

    public void setProperty(Authorizable authorizable, String key, String value) throws RepositoryException {
        Map<String, String> properties = Collections.singletonMap(key, value);
        authorizableManager.updateAuthorizable(authorizable, properties);
    }

    public void setPassword(PasswordOptions options) throws RepositoryException {
        User user = (User) determineAuthorizable(options);
        setPassword(user, options.getPassword());
    }

    public void setPassword(String id, String password) throws RepositoryException {
        User user = authorizableManager.getUser(id);
        setPassword(user, password);
    }

    public void setPassword(User user, String password) throws RepositoryException {
        user.changePassword(password);
    }

    public void save() throws RepositoryException {
        session.save();
    }

    private Authorizable determineAuthorizable(AuthorizableOptions options) throws RepositoryException {
        Authorizable authorizable = options.getAuthorizable();
        if (authorizable == null) {
            authorizable = authorizableManager.getAuthorizable(options.getId());
        }
        return authorizable;
    }
}
