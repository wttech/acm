package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.MyAuthorizable;
import com.wttech.aem.contentor.core.acl.authorizable.UnknownAuthorizable;
import com.wttech.aem.contentor.core.acl.check.CheckAcl;
import com.wttech.aem.contentor.core.acl.utils.AuthorizableManager;
import com.wttech.aem.contentor.core.acl.utils.PermissionsManager;
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
    public User createUser(Closure<CreateUserOptions> closure) {
        return createUser(GroovyUtils.with(new CreateUserOptions(), closure));
    }

    public User createUser(Closure<CreateUserOptions> closure, Closure<MyAuthorizable> action) {
        User user = createUser(GroovyUtils.with(new CreateUserOptions(), closure));
        GroovyUtils.with(forAuthorizable(user), action);
        return user;
    }

    public void forUser(User user, Closure<MyAuthorizable> action) {
        if (!notExists(user)) {
            GroovyUtils.with(forAuthorizable(user), action);
        }
    }

    public void forUser(String id, Closure<MyAuthorizable> action) {
        User user = authorizableManager.getUser(id);
        if (!notExists(user)) {
            GroovyUtils.with(forAuthorizable(user), action);
        }
    }

    public Group createGroup(Closure<CreateGroupOptions> closure) {
        return createGroup(GroovyUtils.with(new CreateGroupOptions(), closure));
    }

    public Group createGroup(Closure<CreateGroupOptions> closure, Closure<MyAuthorizable> action) {
        Group group = createGroup(GroovyUtils.with(new CreateGroupOptions(), closure));
        GroovyUtils.with(forAuthorizable(group), action);
        return group;
    }

    public void forGroup(Group group, Closure<MyAuthorizable> action) {
        if (!notExists(group)) {
            GroovyUtils.with(forAuthorizable(group), action);
        }
    }

    public void forGroup(String id, Closure<MyAuthorizable> action) {
        Group group = authorizableManager.getGroup(id);
        if (!notExists(group)) {
            GroovyUtils.with(forAuthorizable(group), action);
        }
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

    public AclResult setPassword(Closure<PasswordOptions> closure) {
        return setPassword(GroovyUtils.with(new PasswordOptions(), closure));
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
        } else if (options.getMode() == CreateUserOptions.Mode.FAIL) {
        } else if (options.getMode() == CreateUserOptions.Mode.OVERRIDE) {
            authorizableManager.updateUser(user, options.getPassword(), options.determineProperties());
        }
        return user;
    }

    public User createUser(String id) {
        CreateUserOptions options = new CreateUserOptions();
        options.setId(id);
        return createUser(options);
    }

    public User createUser(String id, boolean systemUser) {
        CreateUserOptions options = new CreateUserOptions();
        options.setId(id);
        options.setSystemUser(systemUser);
        return createUser(options);
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
        } else if (options.getMode() == CreateGroupOptions.Mode.FAIL) {
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

    public AclResult deleteUser(AuthorizableOptions options) {
        Authorizable user = determineAuthorizable(options);
        return deleteUser(user);
    }

    public AclResult deleteUser(Authorizable user) {
        AclResult result;
        if (notExists(user)) {
            result = AclResult.ALREADY_DONE;
        } else {
            authorizableManager.deleteAuthorizable(user);
            result = AclResult.DONE;
        }
        return result;
    }

    public AclResult deleteUser(String id) {
        Authorizable user = determineAuthorizable(id);
        return deleteUser(user);
    }

    public AclResult deleteGroup(AuthorizableOptions options) {
        Authorizable group = determineAuthorizable(options);
        return deleteGroup(group);
    }

    public AclResult deleteGroup(Authorizable group) {
        AclResult result;
        if (notExists(group)) {
            result = AclResult.ALREADY_DONE;
        } else {
            authorizableManager.deleteAuthorizable(group);
            result = AclResult.DONE;
        }
        return result;
    }

    public AclResult deleteGroup(String id) {
        Authorizable group = determineAuthorizable(id);
        return deleteGroup(group);
    }

    public AclResult addToGroup(GroupOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        Authorizable group = determineAuthorizable(options.getGroup(), options.getGroupId());
        return addToGroup(authorizable, group);
    }

    public AclResult addToGroup(Authorizable authorizable, Authorizable group) {
        return forAuthorizable(authorizable).addToGroup(group);
    }

    public AclResult addToGroup(String id, String groupId) {
        Authorizable authorizable = determineAuthorizable(id);
        Authorizable group = determineAuthorizable(groupId);
        return addToGroup(authorizable, group);
    }

    public AclResult removeFromGroup(GroupOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        Authorizable group = determineAuthorizable(options.getGroup(), options.getGroupId());
        return removeFromGroup(authorizable, group);
    }

    public AclResult removeFromGroup(Authorizable authorizable, Authorizable group) {
        return forAuthorizable(authorizable).removeFromGroup(group);
    }

    public AclResult removeFromGroup(String id, String groupId) {
        Authorizable authorizable = determineAuthorizable(id);
        Authorizable group = determineAuthorizable(groupId);
        return removeFromGroup(authorizable, group);
    }

    public AclResult removeFromAllGroups(AuthorizableOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        return removeFromAllGroups(authorizable);
    }

    public AclResult removeFromAllGroups(Authorizable authorizable) {
        return forAuthorizable(authorizable).removeFromAllGroups();
    }

    public AclResult removeFromAllGroups(String id) {
        Authorizable authorizable = determineAuthorizable(id);
        return removeFromAllGroups(authorizable);
    }

    public AclResult addMember(MemberOptions options) {
        Authorizable group = determineAuthorizable(options);
        Authorizable member = determineAuthorizable(options.getMember(), options.getMemberId());
        return addMember(group, member);
    }

    public AclResult addMember(Authorizable group, Authorizable member) {
        return forAuthorizable(group).addMember(member);
    }

    public AclResult addMember(String id, String memberId) {
        Authorizable group = determineAuthorizable(id);
        Authorizable member = determineAuthorizable(memberId);
        return addMember(group, member);
    }

    public AclResult removeMember(MemberOptions options) {
        Authorizable group = determineAuthorizable(options);
        Authorizable member = determineAuthorizable(options.getMember(), options.getMemberId());
        return removeMember(group, member);
    }

    public AclResult removeMember(Authorizable group, Authorizable member) {
        return forAuthorizable(group).removeMember(member);
    }

    public AclResult removeMember(String id, String memberId) {
        Authorizable group = determineAuthorizable(id);
        Authorizable member = determineAuthorizable(memberId);
        return removeMember(group, member);
    }

    public AclResult removeAllMembers(AuthorizableOptions options) {
        Authorizable group = determineAuthorizable(options);
        return removeAllMembers(group);
    }

    public AclResult removeAllMembers(Authorizable group) {
        return forAuthorizable(group).removeAllMembers();
    }

    public AclResult removeAllMembers(String id) {
        Authorizable group = determineAuthorizable(id);
        return removeAllMembers(group);
    }

    public AclResult clear(ClearOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        return clear(authorizable, options.getPath(), options.isStrict());
    }

    public AclResult clear(Authorizable authorizable, String path, boolean strict) {
        return forAuthorizable(authorizable).clear(path, strict);
    }

    public AclResult clear(String id, String path, boolean strict) {
        Authorizable authorizable = determineAuthorizable(id);
        return clear(authorizable, path, strict);
    }

    public AclResult clear(Authorizable authorizable, String path) {
        return clear(authorizable, path, false);
    }

    public AclResult clear(String id, String path) {
        Authorizable authorizable = determineAuthorizable(id);
        return clear(authorizable, path, false);
    }

    public AclResult purge(AuthorizableOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        return purge(authorizable);
    }

    public AclResult purge(Authorizable authorizable) {
        return forAuthorizable(authorizable).purge();
    }

    public AclResult purge(String id) {
        Authorizable authorizable = determineAuthorizable(id);
        return purge(authorizable);
    }

    private AclResult apply(
            Authorizable authorizable,
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
        return apply(
                authorizable, path, options.determineAllPermissions(), options.determineAllRestrictions(), mode, allow);
    }

    private AclResult apply(
            Authorizable authorizable,
            String path,
            List<String> permissions,
            Map<String, Object> restrictions,
            PermissionsOptions.Mode mode,
            boolean allow) {
        if (allow) {
            return forAuthorizable(authorizable).allow(path, permissions, restrictions);
        } else {
            return forAuthorizable(authorizable).deny(path, permissions, restrictions);
        }
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
        return apply(authorizable, path, permissions, glob, types, properties, restrictions, mode, true);
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
        Authorizable authorizable = determineAuthorizable(id);
        return allow(authorizable, path, permissions, glob, types, properties, restrictions, mode);
    }

    public AclResult allow(PermissionsOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        return apply(
                authorizable,
                options.getPath(),
                options.determineAllPermissions(),
                options.determineAllRestrictions(),
                options.getMode(),
                true);
    }

    public AclResult allow(Authorizable authorizable, String path, List<String> permissions) {
        return apply(authorizable, path, permissions, Collections.emptyMap(), PermissionsOptions.Mode.SKIP, true);
    }

    public AclResult allow(String id, String path, List<String> permissions) {
        Authorizable authorizable = determineAuthorizable(id);
        return allow(authorizable, path, permissions);
    }

    public AclResult allow(
            Authorizable authorizable, String path, List<String> permissions, Map<String, Object> restrictions) {
        return apply(authorizable, path, permissions, restrictions, PermissionsOptions.Mode.SKIP, true);
    }

    public AclResult allow(String id, String path, List<String> permissions, Map<String, Object> restrictions) {
        Authorizable authorizable = determineAuthorizable(id);
        return allow(authorizable, path, permissions, restrictions);
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
        return apply(authorizable, path, permissions, glob, types, properties, restrictions, mode, false);
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
        Authorizable authorizable = determineAuthorizable(id);
        return deny(authorizable, path, permissions, glob, types, properties, restrictions, mode);
    }

    public AclResult deny(PermissionsOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        return apply(
                authorizable,
                options.getPath(),
                options.determineAllPermissions(),
                options.determineAllRestrictions(),
                options.getMode(),
                false);
    }

    public AclResult deny(Authorizable authorizable, String path, List<String> permissions) {
        return apply(authorizable, path, permissions, Collections.emptyMap(), PermissionsOptions.Mode.SKIP, false);
    }

    public AclResult deny(String id, String path, List<String> permissions) {
        Authorizable authorizable = determineAuthorizable(id);
        return deny(authorizable, path, permissions);
    }

    public AclResult deny(
            Authorizable authorizable, String path, List<String> permissions, Map<String, Object> restrictions) {
        return apply(authorizable, path, permissions, restrictions, PermissionsOptions.Mode.SKIP, false);
    }

    public AclResult deny(String id, String path, List<String> permissions, Map<String, Object> restrictions) {
        Authorizable authorizable = determineAuthorizable(id);
        return deny(authorizable, path, permissions, restrictions);
    }

    public AclResult setProperty(SetPropertyOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        return setProperty(authorizable, options.getRelPath(), options.getValue());
    }

    public AclResult setProperty(Authorizable authorizable, String relPath, String value) {
        return forAuthorizable(authorizable).setProperty(relPath, value);
    }

    public AclResult setProperty(String id, String relPath, String value) {
        Authorizable authorizable = determineAuthorizable(id);
        return setProperty(authorizable, relPath, value);
    }

    public AclResult removeProperty(RemovePropertyOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        return removeProperty(authorizable, options.getRelPath());
    }

    public AclResult removeProperty(Authorizable authorizable, String relPath) {
        return forAuthorizable(authorizable).removeProperty(relPath);
    }

    public AclResult removeProperty(String id, String relPath) {
        Authorizable authorizable = determineAuthorizable(id);
        return removeProperty(authorizable, relPath);
    }

    public AclResult setPassword(PasswordOptions options) {
        Authorizable user = determineAuthorizable(options);
        return setPassword(user, options.getPassword());
    }

    public AclResult setPassword(Authorizable user, String password) {
        return forAuthorizable(user).setPassword(password);
    }

    public AclResult setPassword(String id, String password) {
        Authorizable user = determineAuthorizable(id);
        return setPassword(user, password);
    }

    private Authorizable determineAuthorizable(AuthorizableOptions options) {
        return determineAuthorizable(options.getAuthorizable(), options.getId());
    }

    private Authorizable determineAuthorizable(String id) {
        return determineAuthorizable(null, id);
    }

    private Authorizable determineAuthorizable(Authorizable authorizable, String id) {
        if (authorizable == null && StringUtils.isNotEmpty(id)) {
            authorizable = authorizableManager.getAuthorizable(id);
        }
        if (authorizable == null) {
            authorizable = new UnknownAuthorizable(StringUtils.defaultString(id));
        }
        return authorizable;
    }

    private MyAuthorizable forAuthorizable(Authorizable authorizable) {
        return new MyAuthorizable(
                authorizable, resourceResolver, authorizableManager, permissionsManager, compositeNodeStore);
    }

    private boolean notExists(Authorizable authorizable) {
        return authorizable == null || authorizable instanceof UnknownAuthorizable;
    }
}
