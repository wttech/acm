package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.utils.AuthorizableManager;
import com.wttech.aem.contentor.core.acl.utils.PathUtils;
import com.wttech.aem.contentor.core.acl.utils.PermissionManager;
import com.wttech.aem.contentor.core.acl.utils.PurgeManager;
import com.wttech.aem.contentor.core.acl.utils.RuntimeUtils;
import com.wttech.aem.contentor.core.checkacl.CheckAcl;
import com.wttech.aem.contentor.core.util.GroovyUtils;
import groovy.lang.Closure;
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

    public final CheckAcl check;

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
            this.check = new CheckAcl(resourceResolver);
        } catch (RepositoryException e) {
            throw new AclException("Failed to initialize acl", e);
        }
    }

    // TODO Closure accepting methods need to be defined before the simple ones (add arch unit rule to protect it)
    public User createUser(Closure<CreateUserOptions> closure) {
        return createUser(GroovyUtils.with(new CreateUserOptions(), closure));
    }

    public Group createGroup(Closure<CreateGroupOptions> closure) {
        return createGroup(GroovyUtils.with(new CreateGroupOptions(), closure));
    }

    public void deleteUser(Closure<AuthorizableOptions> closure) {
        deleteUser(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public void deleteGroup(Closure<AuthorizableOptions> closure) {
        deleteGroup(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public void addToGroup(Closure<GroupOptions> closure) {
        addToGroup(GroovyUtils.with(new GroupOptions(), closure));
    }

    public void removeFromGroup(Closure<GroupOptions> closure) {
        removeFromGroup(GroovyUtils.with(new GroupOptions(), closure));
    }

    public void removeFromAllGroups(Closure<AuthorizableOptions> closure) {
        removeFromAllGroups(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public void addMember(Closure<MemberOptions> closure) {
        addMember(GroovyUtils.with(new MemberOptions(), closure));
    }

    public void removeMember(Closure<MemberOptions> closure) {
        removeMember(GroovyUtils.with(new MemberOptions(), closure));
    }

    public void removeAllMembers(Closure<AuthorizableOptions> closure) {
        removeAllMembers(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public void purge(Closure<PurgeOptions> closure) {
        purge(GroovyUtils.with(new PurgeOptions(), closure));
    }

    public AclResult allow(Closure<AllowOptions> closure) {
        return allow(GroovyUtils.with(new AllowOptions(), closure));
    }

    public AclResult deny(Closure<DenyOptions> closure) {
        return deny(GroovyUtils.with(new DenyOptions(), closure));
    }

    public void setProperty(Closure<SetPropertyOptions> closure) {
        setProperty(GroovyUtils.with(new SetPropertyOptions(), closure));
    }

    public void removeProperty(Closure<RemovePropertyOptions> closure) {
        removeProperty(GroovyUtils.with(new RemovePropertyOptions(), closure));
    }

    public void setPassword(Closure<PasswordOptions> closure) {
        setPassword(GroovyUtils.with(new PasswordOptions(), closure));
    }

    public void save(Closure<SaveOptions> closure) {
        save(GroovyUtils.with(new SaveOptions(), closure));
    }

    // Non-closure accepting methods

    public User createUser(
            String id,
            String password,
            String path,
            String givenName,
            String familyName,
            String email,
            String aboutMe,
            boolean systemUser,
            CreateUserOptions.Mode mode) {
        CreateUserOptions options = new CreateUserOptions();
        options.setId(id);
        options.setPassword(password);
        options.setPath(path);
        options.setGivenName(givenName);
        options.setFamilyName(familyName);
        options.setEmail(email);
        options.setAboutMe(aboutMe);
        options.setSystemUser(systemUser);
        options.setMode(mode);
        return createUser(options);
    }

    public User createUser(CreateUserOptions options) {
        User user = authorizableManager.getUser(options.getId());
        if (user == null) {
            if (options.isSystemUser()) {
                user = authorizableManager.createSystemUser(options.getId(), options.getPath());
            } else {
                user = authorizableManager.createUser(options.getId(), options.getPassword(), options.getPath());
            }
            authorizableManager.updateUser(user, options.getPassword(), options.determineProperties());
        } else if (options.getMode() == CreateAuthorizableOptions.Mode.OVERRIDE) {
            authorizableManager.updateUser(user, options.getPassword(), options.determineProperties());
        }
        return user;
    }

    public User createUser(String id) {
        CreateUserOptions options = new CreateUserOptions();
        options.setId(id);
        return createUser(options);
    }

    public User getUser(String id) {
        return authorizableManager.getUser(id);
    }

    public User createSystemUser() {
        throw new IllegalStateException("Not implemented yet!");
    }

    public Group createGroup(
            String id,
            String externalId,
            String path,
            String givenName,
            String email,
            String aboutMe,
            CreateGroupOptions.Mode mode) {
        CreateGroupOptions options = new CreateGroupOptions();
        options.setId(id);
        options.setExternalId(externalId);
        options.setPath(path);
        options.setGivenName(givenName);
        options.setEmail(email);
        options.setAboutMe(aboutMe);
        options.setMode(mode);
        return createGroup(options);
    }

    public Group createGroup(CreateGroupOptions options) {
        Group group = authorizableManager.getGroup(options.getId());
        if (group == null) {
            group = authorizableManager.createGroup(options.getId(), options.getPath(), options.getExternalId());
            authorizableManager.updateGroup(group, options.determineProperties());
        } else if (options.getMode() == CreateGroupOptions.Mode.OVERRIDE) {
            authorizableManager.updateGroup(group, options.determineProperties());
        }
        return group;
    }

    public Group createGroup(String id) {
        CreateGroupOptions options = new CreateGroupOptions();
        options.setId(id);
        return createGroup(options);
    }

    public Group getGroup(String id) {
        return authorizableManager.getGroup(id);
    }

    public void deleteUser(AuthorizableOptions options) {
        User user = (User) determineAuthorizable(options);
        deleteUser(user);
    }

    public void deleteUser(User user) {
        authorizableManager.deleteUser(user);
    }

    public void deleteUser(String id) {
        User user = authorizableManager.getUser(id);
        deleteUser(user);
    }

    public void deleteGroup(AuthorizableOptions options) {
        Group group = (Group) determineAuthorizable(options);
        deleteGroup(group);
    }

    public void deleteGroup(Group group) {
        authorizableManager.deleteGroup(group);
    }

    public void deleteGroup(String id) {
        Group group = authorizableManager.getGroup(id);
        deleteGroup(group);
    }

    public void addToGroup(GroupOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        Group group = (Group) options.getGroupAuthorizable();
        if (group == null) {
            group = authorizableManager.getGroup(options.getGroupId());
        }
        addToGroup(authorizable, group);
    }

    public void addToGroup(Authorizable authorizable, Group group) {
        authorizableManager.addToGroup(authorizable, group);
    }

    public void addToGroup(String id, String groupId) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        Group group = authorizableManager.getGroup(groupId);
        addToGroup(authorizable, group);
    }

    public void removeFromGroup(GroupOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        Group group = (Group) options.getGroupAuthorizable();
        if (group == null) {
            group = authorizableManager.getGroup(options.getGroupId());
        }
        removeFromGroup(authorizable, group);
    }

    public void removeFromGroup(Authorizable authorizable, Group group) {
        authorizableManager.removeFromGroup(authorizable, group);
    }

    public void removeFromGroup(String id, String groupId) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        Group group = authorizableManager.getGroup(groupId);
        removeFromGroup(authorizable, group);
    }

    public void removeFromAllGroups(AuthorizableOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        removeFromAllGroups(authorizable);
    }

    public void removeFromAllGroups(Authorizable authorizable) {
        authorizableManager.removeFromAllGroups(authorizable);
    }

    public void removeFromAllGroups(String id) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        removeFromAllGroups(authorizable);
    }

    public void addMember(MemberOptions options) {
        Group group = (Group) determineAuthorizable(options);
        Authorizable member = options.getMemberAuthorizable();
        if (member == null) {
            member = authorizableManager.getAuthorizable(options.getMemberId());
        }
        addMember(group, member);
    }

    public void addMember(Group group, Authorizable member) {
        addToGroup(member, group);
    }

    public void addMember(String id, String memberId) {
        Group group = authorizableManager.getGroup(id);
        Authorizable member = authorizableManager.getAuthorizable(memberId);
        addMember(group, member);
    }

    public void removeMember(MemberOptions options) {
        Group group = (Group) determineAuthorizable(options);
        Authorizable member = options.getMemberAuthorizable();
        if (member == null) {
            member = authorizableManager.getAuthorizable(options.getMemberId());
        }
        removeMember(group, member);
    }

    public void removeMember(Group group, Authorizable member) {
        authorizableManager.removeMember(group, member);
    }

    public void removeMember(String id, String memberId) {
        Group group = authorizableManager.getGroup(id);
        Authorizable member = authorizableManager.getAuthorizable(memberId);
        removeMember(group, member);
    }

    public void removeAllMembers(AuthorizableOptions options) {
        Group group = (Group) determineAuthorizable(options);
        removeAllMembers(group);
    }

    public void removeAllMembers(Group group) {
        authorizableManager.removeAllMembers(group);
    }

    public void removeAllMembers(String id) {
        Group group = authorizableManager.getGroup(id);
        removeAllMembers(group);
    }

    public void purge(PurgeOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        purge(authorizable, options.getPath(), options.isStrict());
    }

    public AclResult purge(Authorizable authorizable, String path, boolean strict) {
        path = StringUtils.defaultString(path, "/");
        if (compositeNodeStore && PathUtils.isAppsOrLibsPath(path)) {
            return AclResult.SKIPPED;
        } else {
            purgeManager.purge(authorizable, path, strict);
            return AclResult.OK;
        }
    }

    public AclResult purge(String id, String path, boolean strict) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        return purge(authorizable, path, strict);
    }

    public AclResult allow(
            Authorizable authorizable,
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            RestrictionOptions.Mode mode) {
        AllowOptions options = new AllowOptions();
        options.setAuthorizable(authorizable);
        options.setPath(path);
        options.setPermissions(permissions);
        options.setGlob(glob);
        options.setTypes(types);
        options.setProperties(properties);
        options.setRestrictions(restrictions);
        options.setMode(mode);
        return allow(options);
    }

    public AclResult allow(
            String id,
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            RestrictionOptions.Mode mode) {
        AllowOptions options = new AllowOptions();
        options.setId(id);
        options.setPath(path);
        options.setPermissions(permissions);
        options.setGlob(glob);
        options.setTypes(types);
        options.setProperties(properties);
        options.setRestrictions(restrictions);
        options.setMode(mode);
        return allow(options);
    }

    public AclResult allow(AllowOptions options) {
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

    public AclResult allow(Authorizable authorizable, String path, List<String> permissions) {
        AllowOptions options = new AllowOptions();
        options.setAuthorizable(authorizable);
        options.setPath(path);
        options.setPermissions(permissions);
        return allow(options);
    }

    public AclResult allow(String id, String path, List<String> permissions) {
        AllowOptions options = new AllowOptions();
        options.setId(id);
        options.setPath(path);
        options.setPermissions(permissions);
        return allow(options);
    }

    public AclResult deny(
            Authorizable authorizable,
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            RestrictionOptions.Mode mode) {
        DenyOptions options = new DenyOptions();
        options.setAuthorizable(authorizable);
        options.setPath(path);
        options.setPermissions(permissions);
        options.setGlob(glob);
        options.setTypes(types);
        options.setProperties(properties);
        options.setRestrictions(restrictions);
        options.setMode(mode);
        return deny(options);
    }

    public AclResult deny(
            String id,
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            RestrictionOptions.Mode mode) {
        DenyOptions options = new DenyOptions();
        options.setId(id);
        options.setPath(path);
        options.setPermissions(permissions);
        options.setGlob(glob);
        options.setTypes(types);
        options.setProperties(properties);
        options.setRestrictions(restrictions);
        options.setMode(mode);
        return deny(options);
    }

    public AclResult deny(DenyOptions options) {
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

    public AclResult deny(Authorizable authorizable, String path, List<String> permissions) {
        DenyOptions options = new DenyOptions();
        options.setAuthorizable(authorizable);
        options.setPath(path);
        options.setPermissions(permissions);
        return deny(options);
    }

    public AclResult deny(String id, String path, List<String> permissions) {
        DenyOptions options = new DenyOptions();
        options.setId(id);
        options.setPath(path);
        options.setPermissions(permissions);
        return deny(options);
    }

    public void setProperty(SetPropertyOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        setProperty(authorizable, options.getName(), options.getValue());
    }

    public void setProperty(Authorizable authorizable, String name, String value) {
        authorizableManager.setProperty(authorizable, name, value);
    }

    public void setProperty(String id, String name, String value) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        setProperty(authorizable, name, value);
    }

    public void removeProperty(RemovePropertyOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        removeProperty(authorizable, options.getName());
    }

    public void removeProperty(Authorizable authorizable, String name) {
        authorizableManager.removeProperty(authorizable, name);
    }

    public void removeProperty(String id, String name) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        authorizableManager.removeProperty(authorizable, name);
    }

    public void setPassword(PasswordOptions options) {
        User user = (User) determineAuthorizable(options);
        setPassword(user, options.getPassword());
    }

    public void setPassword(User user, String password) {
        authorizableManager.changePassword(user, password);
    }

    public void setPassword(String id, String password) {
        User user = authorizableManager.getUser(id);
        setPassword(user, password);
    }

    public void save(SaveOptions options) {
        save();
    }

    public void save() {
        try {
            session.save();
        } catch (RepositoryException e) {
            throw new AclException("Failed to save", e);
        }
    }

    private Authorizable determineAuthorizable(AuthorizableOptions options) {
        Authorizable authorizable = options.getAuthorizable();
        if (authorizable == null) {
            authorizable = authorizableManager.getAuthorizable(options.getId());
        }
        return authorizable;
    }
}
