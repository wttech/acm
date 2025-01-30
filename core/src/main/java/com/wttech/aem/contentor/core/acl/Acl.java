package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.check.CheckAcl;
import com.wttech.aem.contentor.core.acl.utils.AuthorizableManager;
import com.wttech.aem.contentor.core.acl.utils.PathUtils;
import com.wttech.aem.contentor.core.acl.utils.PermissionsManager;
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

    private final AuthorizableManager authorizableManager;

    private final PermissionsManager permissionsManager;

    private final boolean compositeNodeStore;

    public final CheckAcl check;

    public Acl(ResourceResolver resourceResolver) {
        try {
            JackrabbitSession session = (JackrabbitSession) resourceResolver.adaptTo(Session.class);
            UserManager userManager = session.getUserManager();
            AccessControlManager accessControlManager = session.getAccessControlManager();
            ValueFactory valueFactory = session.getValueFactory();
            this.resourceResolver = resourceResolver;
            this.authorizableManager = new AuthorizableManager(session, userManager, valueFactory);
            this.permissionsManager = new PermissionsManager(session, accessControlManager, valueFactory);
            this.compositeNodeStore = RuntimeUtils.determineCompositeNodeStore(session);
            this.check = new CheckAcl(session, authorizableManager, permissionsManager);
        } catch (RepositoryException e) {
            throw new AclException("Failed to initialize acl", e);
        }
    }

    // TODO Closure accepting methods need to be defined before the simple ones (add arch unit rule to protect it)
    public User forUser(Closure<ForUserOptions> closure) {
        return forUser(GroovyUtils.with(new ForUserOptions(), closure));
    }

    public Group forGroup(Closure<ForGroupOptions> closure) {
        return forGroup(GroovyUtils.with(new ForGroupOptions(), closure));
    }

    public AclResult deleteUser(Closure<AuthorizableOptions> closure) {
        return deleteUser(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public AclResult deleteGroup(Closure<AuthorizableOptions> closure) {
        return deleteGroup(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public AclResult addToGroup(Closure<GroupOptions> closure) {
        return addToGroup(GroovyUtils.with(new GroupOptions(), closure));
    }

    public AclResult removeFromGroup(Closure<GroupOptions> closure) {
        return removeFromGroup(GroovyUtils.with(new GroupOptions(), closure));
    }

    public AclResult removeFromAllGroups(Closure<AuthorizableOptions> closure) {
        return removeFromAllGroups(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public AclResult addMember(Closure<MemberOptions> closure) {
        return addMember(GroovyUtils.with(new MemberOptions(), closure));
    }

    public AclResult removeMember(Closure<MemberOptions> closure) {
        return removeMember(GroovyUtils.with(new MemberOptions(), closure));
    }

    public AclResult removeAllMembers(Closure<AuthorizableOptions> closure) {
        return removeAllMembers(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public AclResult clear(Closure<ClearOptions> closure) {
        return clear(GroovyUtils.with(new ClearOptions(), closure));
    }

    public AclResult purge(Closure<AuthorizableOptions> closure) {
        return purge(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public AclResult allow(Closure<AllowOptions> closure) {
        return allow(GroovyUtils.with(new AllowOptions(), closure));
    }

    public AclResult deny(Closure<DenyOptions> closure) {
        return deny(GroovyUtils.with(new DenyOptions(), closure));
    }

    public AclResult setProperty(Closure<SetPropertyOptions> closure) {
        return setProperty(GroovyUtils.with(new SetPropertyOptions(), closure));
    }

    public AclResult removeProperty(Closure<RemovePropertyOptions> closure) {
        return removeProperty(GroovyUtils.with(new RemovePropertyOptions(), closure));
    }

    public AclResult setPassword(Closure<PasswordOptions> closure) {
        return setPassword(GroovyUtils.with(new PasswordOptions(), closure));
    }

    // Non-closure accepting methods

    public User forUser(
            String id,
            String password,
            String path,
            String givenName,
            String familyName,
            String email,
            String aboutMe,
            boolean systemUser,
            ForUserOptions.Mode mode) {
        ForUserOptions options = new ForUserOptions();
        options.setId(id);
        options.setPassword(password);
        options.setPath(path);
        options.setGivenName(givenName);
        options.setFamilyName(familyName);
        options.setEmail(email);
        options.setAboutMe(aboutMe);
        options.setSystemUser(systemUser);
        options.setMode(mode);
        return forUser(options);
    }

    public User forUser(ForUserOptions options) {
        User user = authorizableManager.getUser(options.getId());
        if (user == null) {
            if (options.isSystemUser()) {
                user = authorizableManager.createSystemUser(options.getId(), options.getPath());
            } else {
                user = authorizableManager.createUser(options.getId(), options.getPassword(), options.getPath());
            }
            authorizableManager.updateUser(user, options.getPassword(), options.determineProperties());
        } else if (options.getMode() == ForUserOptions.Mode.FAIL) {
        } else if (options.getMode() == ForUserOptions.Mode.OVERRIDE) {
            authorizableManager.updateUser(user, options.getPassword(), options.determineProperties());
        }
        return user;
    }

    public User forUser(String id) {
        ForUserOptions options = new ForUserOptions();
        options.setId(id);
        return forUser(options);
    }

    public Group forGroup(
            String id,
            String externalId,
            String path,
            String givenName,
            String email,
            String aboutMe,
            ForGroupOptions.Mode mode) {
        ForGroupOptions options = new ForGroupOptions();
        options.setId(id);
        options.setExternalId(externalId);
        options.setPath(path);
        options.setGivenName(givenName);
        options.setEmail(email);
        options.setAboutMe(aboutMe);
        options.setMode(mode);
        return forGroup(options);
    }

    public Group forGroup(ForGroupOptions options) {
        Group group = authorizableManager.getGroup(options.getId());
        if (group == null) {
            group = authorizableManager.createGroup(options.getId(), options.getPath(), options.getExternalId());
            authorizableManager.updateGroup(group, options.determineProperties());
        } else if (options.getMode() == ForGroupOptions.Mode.FAIL) {
        } else if (options.getMode() == ForGroupOptions.Mode.OVERRIDE) {
            authorizableManager.updateGroup(group, options.determineProperties());
        }
        return group;
    }

    public Group forGroup(String id) {
        ForGroupOptions options = new ForGroupOptions();
        options.setId(id);
        return forGroup(options);
    }

    public AclResult deleteUser(AuthorizableOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        return deleteUser(authorizable);
    }

    public AclResult deleteUser(Authorizable authorizable) {
        if (notExists(authorizable)) {
            return AclResult.ALREADY_DONE;
        }
        authorizableManager.deleteAuthorizable(authorizable);
        return AclResult.DONE;
    }

    public AclResult deleteUser(String id) {
        Authorizable user = authorizableManager.getAuthorizable(id);
        return deleteUser(user);
    }

    public AclResult deleteGroup(AuthorizableOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        return deleteGroup(authorizable);
    }

    public AclResult deleteGroup(Authorizable authorizable) {
        if (notExists(authorizable)) {
            return AclResult.ALREADY_DONE;
        }
        authorizableManager.deleteAuthorizable(authorizable);
        return AclResult.DONE;
    }

    public AclResult deleteGroup(String id) {
        Authorizable group = authorizableManager.getAuthorizable(id);
        return deleteGroup(group);
    }

    public AclResult addToGroup(GroupOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        Authorizable group = determineAuthorizable(options.getGroupAuthorizable(), options.getGroupId());
        return addToGroup(authorizable, group);
    }

    public AclResult addToGroup(Authorizable authorizable, Authorizable group) {
        if (notExists(authorizable) || notExists(group) || !group.isGroup()) {
            return AclResult.SKIPPED;
        }
        return authorizableManager.addMember((Group) group, authorizable) ? AclResult.DONE : AclResult.ALREADY_DONE;
    }

    public AclResult addToGroup(String id, String groupId) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        Authorizable group = authorizableManager.getAuthorizable(groupId);
        return addToGroup(authorizable, group);
    }

    public AclResult removeFromGroup(GroupOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        Authorizable group = determineAuthorizable(options.getGroupAuthorizable(), options.getGroupId());
        return removeFromGroup(authorizable, group);
    }

    public AclResult removeFromGroup(Authorizable authorizable, Authorizable group) {
        if (notExists(authorizable) || !group.isGroup()) {
            return AclResult.SKIPPED;
        }
        return authorizableManager.removeMember((Group) group, authorizable) ? AclResult.DONE : AclResult.ALREADY_DONE;
    }

    public AclResult removeFromGroup(String id, String groupId) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        Authorizable group = authorizableManager.getAuthorizable(groupId);
        return removeFromGroup(authorizable, group);
    }

    public AclResult removeFromAllGroups(AuthorizableOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        return removeFromAllGroups(authorizable);
    }

    public AclResult removeFromAllGroups(Authorizable authorizable) {
        if (notExists(authorizable)) {
            return AclResult.SKIPPED;
        }
        try {
            Iterator<Group> groups = authorizable.memberOf();
            AclResult result = groups.hasNext() ? AclResult.DONE : AclResult.ALREADY_DONE;
            while (groups.hasNext()) {
                authorizableManager.removeMember(groups.next(), authorizable);
            }
            return result;
        } catch (RepositoryException e) {
            throw new AclException("Failed to remove authorizable from all groups", e);
        }
    }

    public AclResult removeFromAllGroups(String id) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        return removeFromAllGroups(authorizable);
    }

    public AclResult addMember(MemberOptions options) {
        Authorizable group = determineAuthorizable(options);
        Authorizable member = determineAuthorizable(options.getMemberAuthorizable(), options.getMemberId());
        return addMember(group, member);
    }

    public AclResult addMember(Authorizable group, Authorizable member) {
        if (notExists(group) || notExists(member) || !group.isGroup()) {
            return AclResult.SKIPPED;
        }
        return authorizableManager.addMember((Group) group, member) ? AclResult.DONE : AclResult.ALREADY_DONE;
    }

    public AclResult addMember(String id, String memberId) {
        Authorizable group = authorizableManager.getAuthorizable(id);
        Authorizable member = authorizableManager.getAuthorizable(memberId);
        return addMember(group, member);
    }

    public AclResult removeMember(MemberOptions options) {
        Authorizable group = determineAuthorizable(options);
        Authorizable member = determineAuthorizable(options.getMemberAuthorizable(), options.getMemberId());
        return removeMember(group, member);
    }

    public AclResult removeMember(Authorizable group, Authorizable member) {
        if (notExists(group) || !group.isGroup()) {
            return AclResult.SKIPPED;
        }
        return authorizableManager.removeMember((Group) group, member) ? AclResult.DONE : AclResult.ALREADY_DONE;
    }

    public AclResult removeMember(String id, String memberId) {
        Authorizable group = authorizableManager.getAuthorizable(id);
        Authorizable member = authorizableManager.getAuthorizable(memberId);
        return removeMember(group, member);
    }

    public AclResult removeAllMembers(AuthorizableOptions options) {
        Authorizable group = determineAuthorizable(options);
        return removeAllMembers(group);
    }

    public AclResult removeAllMembers(Authorizable group) {
        if (notExists(group) || !group.isGroup()) {
            return AclResult.SKIPPED;
        }
        try {
            Iterator<Authorizable> members = ((Group) group).getMembers();
            AclResult result = members.hasNext() ? AclResult.DONE : AclResult.ALREADY_DONE;
            while (members.hasNext()) {
                authorizableManager.removeMember((Group) group, members.next());
            }
            return result;
        } catch (RepositoryException e) {
            throw new AclException("Failed to remove all members from group", e);
        }
    }

    public AclResult removeAllMembers(String id) {
        Authorizable group = authorizableManager.getAuthorizable(id);
        return removeAllMembers(group);
    }

    public AclResult clear(ClearOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        return clear(authorizable, options.getPath(), options.isStrict());
    }

    public AclResult clear(Authorizable authorizable, String path, boolean strict) {
        if (notExists(authorizable)) {
            return AclResult.SKIPPED;
        }
        path = StringUtils.defaultString(path, "/");
        if (compositeNodeStore && PathUtils.isAppsOrLibsPath(path)) {
            return AclResult.SKIPPED;
        }
        return permissionsManager.clear(authorizable, path, strict) ? AclResult.DONE : AclResult.ALREADY_DONE;
    }

    public AclResult clear(String id, String path, boolean strict) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        return clear(authorizable, path, strict);
    }

    public AclResult purge(AuthorizableOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        return purge(authorizable);
    }

    public AclResult purge(Authorizable authorizable) {
        if (notExists(authorizable)) {
            return AclResult.SKIPPED;
        }
        AclResult result = AclResult.ALREADY_DONE;
        if (authorizable.isGroup() && removeAllMembers(authorizable) != AclResult.ALREADY_DONE) {
            result = AclResult.DONE;
        }
        if (removeFromAllGroups(authorizable) != AclResult.ALREADY_DONE) {
            result = AclResult.DONE;
        }
        if (clear(authorizable, "/", false) != AclResult.ALREADY_DONE) {
            result = AclResult.DONE;
        }
        return result;
    }

    public AclResult clear(String id) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        return purge(authorizable);
    }

    public AclResult allow(
            Authorizable authorizable,
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            PermissionsOptions.Mode mode) {
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
            PermissionsOptions.Mode mode) {
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
        Authorizable authorizable = determineAuthorizable(options);
        if (notExists(authorizable)) {
            return AclResult.SKIPPED;
        }
        Resource resource = resourceResolver.getResource(options.getPath());
        if (resource == null) {
            if (options.getMode() == PermissionsOptions.Mode.FAIL) {
                throw new AclException(String.format("Path %s not found", options.getPath()));
            }
            return AclResult.SKIPPED;
        } else if (compositeNodeStore && PathUtils.isAppsOrLibsPath(options.getPath())) {
            return AclResult.SKIPPED;
        } else {
            if (check.allow(options)) {
                return AclResult.ALREADY_DONE;
            }
            permissionsManager.apply(
                    authorizable,
                    options.getPath(),
                    options.determineAllPermissions(),
                    options.determineAllRestrictions(),
                    true);
            return AclResult.DONE;
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
            PermissionsOptions.Mode mode) {
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
            PermissionsOptions.Mode mode) {
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
        Authorizable authorizable = determineAuthorizable(options);
        if (notExists(authorizable)) {
            return AclResult.SKIPPED;
        }
        Resource resource = resourceResolver.getResource(options.getPath());
        if (resource == null) {
            if (options.getMode() == PermissionsOptions.Mode.FAIL) {
                throw new AclException(String.format("Path %s not found", options.getPath()));
            }
            return AclResult.SKIPPED;
        } else if (compositeNodeStore && PathUtils.isAppsOrLibsPath(options.getPath())) {
            return AclResult.SKIPPED;
        } else {
            if (check.deny(options)) {
                return AclResult.ALREADY_DONE;
            }
            permissionsManager.apply(
                    authorizable,
                    options.getPath(),
                    options.determineAllPermissions(),
                    options.determineAllRestrictions(),
                    false);
            return AclResult.DONE;
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

    public AclResult setProperty(SetPropertyOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        return setProperty(authorizable, options.getRelPath(), options.getValue());
    }

    public AclResult setProperty(Authorizable authorizable, String relPath, String value) {
        if (notExists(authorizable)) {
            return AclResult.SKIPPED;
        }
        if (!check.property(getAuthorizableId(authorizable), relPath, value)) {
            authorizableManager.setProperty(authorizable, relPath, value);
            return AclResult.DONE;
        }
        return AclResult.ALREADY_DONE;
    }

    public AclResult setProperty(String id, String relPath, String value) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        return setProperty(authorizable, relPath, value);
    }

    public AclResult removeProperty(RemovePropertyOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        return removeProperty(authorizable, options.getRelPath());
    }

    public AclResult removeProperty(Authorizable authorizable, String relPath) {
        if (notExists(authorizable)) {
            return AclResult.SKIPPED;
        }
        return authorizableManager.removeProperty(authorizable, relPath) ? AclResult.DONE : AclResult.ALREADY_DONE;
    }

    public AclResult removeProperty(String id, String relPath) {
        Authorizable authorizable = authorizableManager.getAuthorizable(id);
        return removeProperty(authorizable, relPath);
    }

    public AclResult setPassword(PasswordOptions options) {
        Authorizable user = determineAuthorizable(options);
        return setPassword(user, options.getPassword());
    }

    public AclResult setPassword(Authorizable user, String password) {
        if (notExists(user)) {
            return AclResult.SKIPPED;
        }
        if (!check.password(getAuthorizableId(user), password)) {
            authorizableManager.changePassword((User) user, password);
            return AclResult.DONE;
        }
        return AclResult.ALREADY_DONE;
    }

    public AclResult setPassword(String id, String password) {
        Authorizable user = authorizableManager.getAuthorizable(id);
        return setPassword(user, password);
    }

    private Authorizable determineAuthorizable(AuthorizableOptions options) {
        return determineAuthorizable(options.getAuthorizable(), options.getId());
    }

    private Authorizable determineAuthorizable(Authorizable authorizable, String id) {
        if (authorizable == null) {
            authorizable = authorizableManager.getAuthorizable(id);
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

    private boolean notExists(Authorizable authorizable) {
        return authorizable == null || check.notExists(getAuthorizableId(authorizable));
    }
}
