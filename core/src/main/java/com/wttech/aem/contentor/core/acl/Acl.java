package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.utils.AuthorizableManager;
import com.wttech.aem.contentor.core.acl.utils.PathUtils;
import com.wttech.aem.contentor.core.acl.utils.PermissionManager;
import com.wttech.aem.contentor.core.acl.utils.PurgeManager;
import com.wttech.aem.contentor.core.acl.utils.RuntimeUtils;
import com.wttech.aem.contentor.core.util.GroovyUtils;
import groovy.lang.Closure;
import java.util.Iterator;
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

    public void deleteUser(Closure<AuthorizableOptions> closure) throws RepositoryException {
        deleteUser(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public void deleteGroup(Closure<AuthorizableOptions> closure) throws RepositoryException {
        deleteGroup(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public void addToGroup(Closure<GroupOptions> closure) throws RepositoryException {
        addToGroup(GroovyUtils.with(new GroupOptions(), closure));
    }

    public void removeFromGroup(Closure<GroupOptions> closure) throws RepositoryException {
        removeFromGroup(GroovyUtils.with(new GroupOptions(), closure));
    }

    public void removeFromAllGroups(Closure<AuthorizableOptions> closure) throws RepositoryException {
        removeFromAllGroups(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public void addMember(Closure<MemberOptions> closure) throws RepositoryException {
        addMember(GroovyUtils.with(new MemberOptions(), closure));
    }

    public void removeMember(Closure<MemberOptions> closure) throws RepositoryException {
        removeMember(GroovyUtils.with(new MemberOptions(), closure));
    }

    public void removeAllMembers(Closure<AuthorizableOptions> closure) throws RepositoryException {
        removeAllMembers(GroovyUtils.with(new AuthorizableOptions(), closure));
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

    public void setProperty(Closure<SetPropertyOptions> closure) throws RepositoryException {
        setProperty(GroovyUtils.with(new SetPropertyOptions(), closure));
    }

    public void removeProperty(Closure<RemovePropertyOptions> closure) throws RepositoryException {
        removeProperty(GroovyUtils.with(new RemovePropertyOptions(), closure));
    }

    public void setPassword(Closure<PasswordOptions> closure) throws RepositoryException {
        setPassword(GroovyUtils.with(new PasswordOptions(), closure));
    }

    public void save(Closure<SaveOptions> closure) throws RepositoryException {
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
            CreateUserOptions.Mode mode)
            throws RepositoryException {
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

    public User createUser(CreateUserOptions options) throws RepositoryException {
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

    public User createUser(String id) throws RepositoryException {
        CreateUserOptions options = new CreateUserOptions();
        options.setId(id);
        return createUser(options);
    }

    public User getUser(String id) throws RepositoryException {
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
            CreateGroupOptions.Mode mode)
            throws RepositoryException {
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

    public Group createGroup(CreateGroupOptions options) throws RepositoryException {
        Group group = authorizableManager.getGroup(options.getId());
        if (group == null) {
            group = authorizableManager.createGroup(options.getId(), options.getPath(), options.getExternalId());
            authorizableManager.updateGroup(group, options.determineProperties());
        } else if (options.getMode() == CreateGroupOptions.Mode.OVERRIDE) {
            authorizableManager.updateGroup(group, options.determineProperties());
        }
        return group;
    }

    public Group createGroup(String id) throws RepositoryException {
        CreateGroupOptions options = new CreateGroupOptions();
        options.setId(id);
        return createGroup(options);
    }

    public Group getGroup(String id) throws RepositoryException {
        return authorizableManager.getGroup(id);
    }

    public void deleteUser(AuthorizableOptions options) throws RepositoryException {
        User user = (User) determineAuthorizable(options);
        deleteUser(user);
    }

    public void deleteUser(User user) throws RepositoryException {
        authorizableManager.deleteUser(user);
    }

    public void deleteUser(String id) throws RepositoryException {
        User user = authorizableManager.getUser(id);
        deleteUser(user);
    }

    public void deleteGroup(AuthorizableOptions options) throws RepositoryException {
        Group group = (Group) determineAuthorizable(options);
        deleteGroup(group);
    }

    public void deleteGroup(Group group) throws RepositoryException {
        authorizableManager.deleteGroup(group);
    }

    public void deleteGroup(String id) throws RepositoryException {
        Group group = authorizableManager.getGroup(id);
        deleteGroup(group);
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

    public void removeFromAllGroups(AuthorizableOptions options) throws RepositoryException {
        Authorizable authorizable = determineAuthorizable(options);
        removeFromAllGroups(authorizable);
    }

    public void removeFromAllGroups(Authorizable authorizable) throws RepositoryException {
        Iterator<Group> groups = authorizable.memberOf();
        while (groups.hasNext()) {
            Group group = groups.next();
            removeFromGroup(authorizable, group);
        }
    }

    public void removeFromAllGroups(String id) throws RepositoryException {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        removeFromAllGroups(authorizable);
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

    public void removeAllMembers(AuthorizableOptions options) throws RepositoryException {
        Group group = (Group) determineAuthorizable(options);
        removeAllMembers(group);
    }

    public void removeAllMembers(Group group) throws RepositoryException {
        Iterator<Authorizable> members = group.getMembers();
        while (members.hasNext()) {
            Authorizable member = members.next();
            removeFromGroup(member, group);
        }
    }

    public void removeAllMembers(String id) throws RepositoryException {
        Group group = authorizableManager.getGroup(id);
        removeAllMembers(group);
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

    public AclResult purge(String id, String path, boolean strict) throws RepositoryException {
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
            RestrictionOptions.Mode mode)
            throws RepositoryException {
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
            RestrictionOptions.Mode mode)
            throws RepositoryException {
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

    public AclResult deny(
            Authorizable authorizable,
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            RestrictionOptions.Mode mode)
            throws RepositoryException {
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
            RestrictionOptions.Mode mode)
            throws RepositoryException {
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

    public void setProperty(SetPropertyOptions options) throws RepositoryException {
        Authorizable authorizable = determineAuthorizable(options);
        setProperty(authorizable, options.getName(), options.getValue());
    }

    public void setProperty(Authorizable authorizable, String name, String value) throws RepositoryException {
        authorizableManager.setProperty(authorizable, name, value);
    }

    public void setProperty(String id, String name, String value) throws RepositoryException {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        setProperty(authorizable, name, value);
    }

    public void removeProperty(RemovePropertyOptions options) throws RepositoryException {
        Authorizable authorizable = determineAuthorizable(options);
        removeProperty(authorizable, options.getName());
    }

    public void removeProperty(Authorizable authorizable, String name) throws RepositoryException {
        authorizableManager.removeProperty(authorizable, name);
    }

    public void removeProperty(String id, String name) throws RepositoryException {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        authorizableManager.removeProperty(authorizable, name);
    }

    public void setPassword(PasswordOptions options) throws RepositoryException {
        User user = (User) determineAuthorizable(options);
        setPassword(user, options.getPassword());
    }

    public void setPassword(User user, String password) throws RepositoryException {
        user.changePassword(password);
    }

    public void setPassword(String id, String password) throws RepositoryException {
        User user = authorizableManager.getUser(id);
        setPassword(user, password);
    }

    public void save(SaveOptions options) throws RepositoryException {
        save();
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
