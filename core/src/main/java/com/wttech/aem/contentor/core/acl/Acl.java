package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.utils.AuthorizableManager;
import com.wttech.aem.contentor.core.acl.utils.PathUtils;
import com.wttech.aem.contentor.core.acl.utils.PermissionManager;
import com.wttech.aem.contentor.core.acl.utils.PurgeManager;
import com.wttech.aem.contentor.core.acl.utils.RuntimeUtils;
import com.wttech.aem.contentor.core.checkacl.CheckAcl;
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

    public BasicResult deleteUser(Closure<AuthorizableOptions> closure) {
        return deleteUser(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public BasicResult deleteGroup(Closure<AuthorizableOptions> closure) {
        return deleteGroup(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public BasicResult addToGroup(Closure<GroupOptions> closure) {
        return addToGroup(GroovyUtils.with(new GroupOptions(), closure));
    }

    public BasicResult removeFromGroup(Closure<GroupOptions> closure) {
        return removeFromGroup(GroovyUtils.with(new GroupOptions(), closure));
    }

    public BasicResult removeFromAllGroups(Closure<AuthorizableOptions> closure) {
        return removeFromAllGroups(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public BasicResult addMember(Closure<MemberOptions> closure) {
        return addMember(GroovyUtils.with(new MemberOptions(), closure));
    }

    public BasicResult removeMember(Closure<MemberOptions> closure) {
        return removeMember(GroovyUtils.with(new MemberOptions(), closure));
    }

    public BasicResult removeAllMembers(Closure<AuthorizableOptions> closure) {
        return removeAllMembers(GroovyUtils.with(new AuthorizableOptions(), closure));
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

    public BasicResult setProperty(Closure<SetPropertyOptions> closure) {
        return setProperty(GroovyUtils.with(new SetPropertyOptions(), closure));
    }

    public BasicResult removeProperty(Closure<RemovePropertyOptions> closure) {
        return removeProperty(GroovyUtils.with(new RemovePropertyOptions(), closure));
    }

    public BasicResult setPassword(Closure<PasswordOptions> closure) {
        return setPassword(GroovyUtils.with(new PasswordOptions(), closure));
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

    public BasicResult deleteUser(AuthorizableOptions options) {
        User user = (User) determineAuthorizable(options);
        return deleteUser(user);
    }

    public BasicResult deleteUser(User user) {
        if (user == null || check.notExists(getAuthorizableId(user))) {
            return BasicResult.ALREADY_DONE;
        }
        authorizableManager.deleteUser(user);
        return BasicResult.DONE;
    }

    public BasicResult deleteUser(String id) {
        User user = authorizableManager.getUser(id);
        return deleteUser(user);
    }

    public BasicResult deleteGroup(AuthorizableOptions options) {
        Group group = (Group) determineAuthorizable(options);
        return deleteGroup(group);
    }

    public BasicResult deleteGroup(Group group) {
        if (group == null || check.notExists(getAuthorizableId(group))) {
            return BasicResult.ALREADY_DONE;
        }
        authorizableManager.deleteGroup(group);
        return BasicResult.DONE;
    }

    public BasicResult deleteGroup(String id) {
        Group group = authorizableManager.getGroup(id);
        return deleteGroup(group);
    }

    public BasicResult addToGroup(GroupOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        Group group = (Group) options.getGroupAuthorizable();
        if (group == null) {
            group = authorizableManager.getGroup(options.getGroupId());
        }
        return addToGroup(authorizable, group);
    }

    public BasicResult addToGroup(Authorizable authorizable, Group group) {
        return authorizableManager.addToGroup(authorizable, group) ? BasicResult.DONE : BasicResult.ALREADY_DONE;
    }

    public BasicResult addToGroup(String id, String groupId) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        Group group = authorizableManager.getGroup(groupId);
        return addToGroup(authorizable, group);
    }

    public BasicResult removeFromGroup(GroupOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        Group group = (Group) options.getGroupAuthorizable();
        if (group == null) {
            group = authorizableManager.getGroup(options.getGroupId());
        }
        return removeFromGroup(authorizable, group);
    }

    public BasicResult removeFromGroup(Authorizable authorizable, Group group) {
        return authorizableManager.removeFromGroup(authorizable, group) ? BasicResult.DONE : BasicResult.ALREADY_DONE;
    }

    public BasicResult removeFromGroup(String id, String groupId) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        Group group = authorizableManager.getGroup(groupId);
        return removeFromGroup(authorizable, group);
    }

    public BasicResult removeFromAllGroups(AuthorizableOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        return removeFromAllGroups(authorizable);
    }

    public BasicResult removeFromAllGroups(Authorizable authorizable) {
        try {
            Iterator<Group> groups = authorizable.memberOf();
            BasicResult result = groups.hasNext() ? BasicResult.DONE : BasicResult.ALREADY_DONE;
            while (groups.hasNext()) {
                removeFromGroup(authorizable, groups.next());
            }
            return result;
        } catch (RepositoryException e) {
            throw new AclException("Failed to remove authorizable from all groups", e);
        }
    }

    public BasicResult removeFromAllGroups(String id) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        return removeFromAllGroups(authorizable);
    }

    public BasicResult addMember(MemberOptions options) {
        Group group = (Group) determineAuthorizable(options);
        Authorizable member = options.getMemberAuthorizable();
        if (member == null) {
            member = authorizableManager.getAuthorizable(options.getMemberId());
        }
        return addMember(group, member);
    }

    public BasicResult addMember(Group group, Authorizable member) {
        return addToGroup(member, group);
    }

    public BasicResult addMember(String id, String memberId) {
        Group group = authorizableManager.getGroup(id);
        Authorizable member = authorizableManager.getAuthorizable(memberId);
        return addMember(group, member);
    }

    public BasicResult removeMember(MemberOptions options) {
        Group group = (Group) determineAuthorizable(options);
        Authorizable member = options.getMemberAuthorizable();
        if (member == null) {
            member = authorizableManager.getAuthorizable(options.getMemberId());
        }
        return removeMember(group, member);
    }

    public BasicResult removeMember(Group group, Authorizable member) {
        return removeFromGroup(member, group);
    }

    public BasicResult removeMember(String id, String memberId) {
        Group group = authorizableManager.getGroup(id);
        Authorizable member = authorizableManager.getAuthorizable(memberId);
        return removeMember(group, member);
    }

    public BasicResult removeAllMembers(AuthorizableOptions options) {
        Group group = (Group) determineAuthorizable(options);
        return removeAllMembers(group);
    }

    public BasicResult removeAllMembers(Group group) {
        try {
            Iterator<Authorizable> members = group.getMembers();
            BasicResult result = members.hasNext() ? BasicResult.DONE : BasicResult.ALREADY_DONE;
            while (members.hasNext()) {
                removeMember(group, members.next());
            }
            return result;
        } catch (RepositoryException e) {
            throw new AclException("Failed to remove all members from group", e);
        }
    }

    public BasicResult removeAllMembers(String id) {
        Group group = authorizableManager.getGroup(id);
        return removeAllMembers(group);
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

    public BasicResult setProperty(SetPropertyOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        return setProperty(authorizable, options.getName(), options.getValue());
    }

    public BasicResult setProperty(Authorizable authorizable, String name, String value) {
        if (!check.property(getAuthorizableId(authorizable), name, value)) {
            authorizableManager.setProperty(authorizable, name, value);
            return BasicResult.DONE;
        }
        return BasicResult.ALREADY_DONE;
    }

    public BasicResult setProperty(String id, String name, String value) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        return setProperty(authorizable, name, value);
    }

    public BasicResult removeProperty(RemovePropertyOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        return removeProperty(authorizable, options.getName());
    }

    public BasicResult removeProperty(Authorizable authorizable, String name) {
        return authorizableManager.removeProperty(authorizable, name) ? BasicResult.DONE : BasicResult.ALREADY_DONE;
    }

    public BasicResult removeProperty(String id, String name) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        return removeProperty(authorizable, name);
    }

    public BasicResult setPassword(PasswordOptions options) {
        User user = (User) determineAuthorizable(options);
        return setPassword(user, options.getPassword());
    }

    public BasicResult setPassword(User user, String password) {
        if (!check.password(getAuthorizableId(user), password)) {
            authorizableManager.changePassword(user, password);
            return BasicResult.DONE;
        }
        return BasicResult.ALREADY_DONE;
    }

    public BasicResult setPassword(String id, String password) {
        User user = authorizableManager.getUser(id);
        return setPassword(user, password);
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

    private String getAuthorizableId(Authorizable authorizable) {
        try {
            return authorizable.getID();
        } catch (RepositoryException e) {
            throw new AclException("Failed to get authorizable ID", e);
        }
    }
}
