package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.MyAuthorizable;
import com.wttech.aem.contentor.core.acl.authorizable.MyGroup;
import com.wttech.aem.contentor.core.acl.authorizable.MyUser;
import com.wttech.aem.contentor.core.acl.authorizable.UnknownAuthorizable;
import com.wttech.aem.contentor.core.acl.check.CheckAcl;
import com.wttech.aem.contentor.core.acl.utils.AuthorizableManager;
import com.wttech.aem.contentor.core.acl.utils.PermissionsManager;
import com.wttech.aem.contentor.core.acl.utils.RuntimeUtils;
import com.wttech.aem.contentor.core.util.GroovyUtils;
import groovy.lang.Closure;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlManager;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.helpers.MessageFormatter;

public class Acl {

    private final ResourceResolver resourceResolver;

    private final AuthorizableManager authorizableManager;

    private final PermissionsManager permissionsManager;

    private final boolean compositeNodeStore;

    public final CheckAcl check;

    private final OutputStream out;

    public Acl(ResourceResolver resourceResolver, OutputStream out) {
        try {
            JackrabbitSession session = (JackrabbitSession) resourceResolver.adaptTo(Session.class);
            UserManager userManager = session.getUserManager();
            AccessControlManager accessControlManager = session.getAccessControlManager();
            ValueFactory valueFactory = session.getValueFactory();
            this.resourceResolver = resourceResolver;
            this.authorizableManager = new AuthorizableManager(session, userManager, valueFactory);
            this.permissionsManager = new PermissionsManager(session, accessControlManager, valueFactory);
            this.compositeNodeStore = RuntimeUtils.determineCompositeNodeStore(session);
            this.check = new CheckAcl(authorizableManager, permissionsManager);
            this.out = out;
        } catch (RepositoryException e) {
            throw new AclException("Failed to initialize acl", e);
        }
    }

    // TODO Closure accepting methods need to be defined before the simple ones (add arch unit rule to protect it)
    public MyUser createUser(Closure<CreateUserOptions> closure) {
        return createUser(GroovyUtils.with(new CreateUserOptions(), closure));
    }

    public AclResult createUser(Closure<CreateUserOptions> closure, Closure<MyUser> action) {
        MyUser user = createUser(closure);
        return user.with(action);
    }

    public MyGroup createGroup(Closure<CreateGroupOptions> closure) {
        return createGroup(GroovyUtils.with(new CreateGroupOptions(), closure));
    }

    public AclResult createGroup(Closure<CreateGroupOptions> closure, Closure<MyGroup> action) {
        MyGroup group = createGroup(closure);
        return group.with(action);
    }

    public MyUser forUser(Closure<AuthorizableOptions> closure) {
        return forUser(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public AclResult forUser(Closure<AuthorizableOptions> closure, Closure<MyUser> action) {
        MyUser user = forUser(closure);
        return user.with(action);
    }

    public MyGroup forGroup(Closure<AuthorizableOptions> closure) {
        return forGroup(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public AclResult forGroup(Closure<AuthorizableOptions> closure, Closure<MyGroup> action) {
        MyGroup group = forGroup(closure);
        return group.with(action);
    }

    public MyUser getUser(Closure<AuthorizableOptions> closure) {
        return getUser(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public MyGroup getGroup(Closure<AuthorizableOptions> closure) {
        return getGroup(GroovyUtils.with(new AuthorizableOptions(), closure));
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

    public MyUser createUser(
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

    public MyUser createUser(CreateUserOptions options) {
        logResult(options.getId(), "createUser");
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
        return forMyUser(user);
    }

    public MyUser createUser(String id) {
        CreateUserOptions options = new CreateUserOptions();
        options.setId(id);
        return createUser(options);
    }

    public MyUser createSystemUser(String id) {
        CreateUserOptions options = new CreateUserOptions();
        options.setId(id);
        options.systemUser();
        return createUser(options);
    }

    public MyGroup createGroup(
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

    public MyGroup createGroup(CreateGroupOptions options) {
        logResult(options.getId(), "createGroup");
        Group group = authorizableManager.getGroup(options.getId());
        if (group == null) {
            group = authorizableManager.createGroup(options.getId(), options.getPath(), options.getExternalId());
            authorizableManager.updateGroup(group, options.determineProperties());
        } else if (options.getMode() == CreateGroupOptions.Mode.FAIL) {
        } else if (options.getMode() == CreateGroupOptions.Mode.OVERRIDE) {
            authorizableManager.updateGroup(group, options.determineProperties());
        }
        return forMyGroup(group);
    }

    public MyGroup createGroup(String id) {
        CreateGroupOptions options = new CreateGroupOptions();
        options.setId(id);
        return createGroup(options);
    }

    public MyGroup createGroup(String id, String externalId) {
        CreateGroupOptions options = new CreateGroupOptions();
        options.setId(id);
        options.setExternalId(externalId);
        return createGroup(options);
    }

    public MyUser forUser(Object userObj) {
        Authorizable user = determineAuthorizable(userObj);
        logResult(user, "forUser");
        return forMyUser(user);
    }

    public MyGroup forGroup(Object groupObj) {
        Authorizable group = determineAuthorizable(groupObj);
        logResult(group, "forGroup");
        return forMyGroup(group);
    }

    public MyUser getUser(Object userObj) {
        Authorizable user = determineAuthorizable(userObj);
        logResult(user, "getUser");
        return forMyUser(user);
    }

    public MyGroup getGroup(Object groupObj) {
        Authorizable group = determineAuthorizable(groupObj);
        logResult(group, "getGroup");
        return forMyGroup(group);
    }

    public AclResult deleteUser(Object userObj) {
        Authorizable user = determineAuthorizable(userObj);
        String userId = getID(user);
        AclResult result;
        if (notExists(user)) {
            result = AclResult.ALREADY_DONE;
        } else {
            authorizableManager.deleteAuthorizable(user);
            result = AclResult.DONE;
        }
        logResult(userId, "deleteUser {}", result);
        return result;
    }

    public AclResult deleteGroup(Object groupObj) {
        Authorizable group = determineAuthorizable(groupObj);
        String groupId = getID(group);
        AclResult result;
        if (notExists(group)) {
            result = AclResult.ALREADY_DONE;
        } else {
            authorizableManager.deleteAuthorizable(group);
            result = AclResult.DONE;
        }
        logResult(groupId, "deleteGroup {}", result);
        return result;
    }

    public AclResult addToGroup(GroupOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        Authorizable group = determineAuthorizable(options.getGroup(), options.getGroupId());
        return addToGroup(authorizable, group);
    }

    public AclResult addToGroup(Object authorizableObj, Object groupObj) {
        Authorizable authorizable = determineAuthorizable(authorizableObj);
        Authorizable group = determineAuthorizable(groupObj);
        return forMyAuthorizable(authorizable).addToGroup(group);
    }

    public AclResult removeFromGroup(GroupOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        Authorizable group = determineAuthorizable(options.getGroup(), options.getGroupId());
        return removeFromGroup(authorizable, group);
    }

    public AclResult removeFromGroup(Object authorizableObj, Object groupObj) {
        Authorizable authorizable = determineAuthorizable(authorizableObj);
        Authorizable group = determineAuthorizable(groupObj);
        return forMyAuthorizable(authorizable).removeFromGroup(group);
    }

    public AclResult removeFromAllGroups(Object authorizableObj) {
        Authorizable authorizable = determineAuthorizable(authorizableObj);
        return forMyAuthorizable(authorizable).removeFromAllGroups();
    }

    public AclResult addMember(MemberOptions options) {
        Authorizable group = determineAuthorizable(options);
        Authorizable member = determineAuthorizable(options.getMember(), options.getMemberId());
        return addMember(group, member);
    }

    public AclResult addMember(Object groupObj, Object memberObj) {
        Authorizable group = determineAuthorizable(groupObj);
        Authorizable member = determineAuthorizable(memberObj);
        return forMyGroup(group).addMember(member);
    }

    public AclResult removeMember(MemberOptions options) {
        Authorizable group = determineAuthorizable(options);
        Authorizable member = determineAuthorizable(options.getMember(), options.getMemberId());
        return removeMember(group, member);
    }

    public AclResult removeMember(Object groupObj, Object memberObj) {
        Authorizable group = determineAuthorizable(groupObj);
        Authorizable member = determineAuthorizable(memberObj);
        return forMyGroup(group).removeMember(member);
    }

    public AclResult removeAllMembers(Object groupObj) {
        Authorizable group = determineAuthorizable(groupObj);
        return forMyGroup(group).removeAllMembers();
    }

    public AclResult clear(ClearOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        return clear(authorizable, options.getPath(), options.isStrict());
    }

    public AclResult clear(Object authorizableObj, String path, boolean strict) {
        Authorizable authorizable = determineAuthorizable(authorizableObj);
        return forMyAuthorizable(authorizable).clear(path, strict);
    }

    public AclResult clear(Object authorizableObj, String path) {
        return clear(authorizableObj, path, false);
    }

    public AclResult purge(Object authorizableObj) {
        Authorizable authorizable = determineAuthorizable(authorizableObj);
        return authorizable.isGroup()
                ? forMyGroup(authorizable).purge()
                : forMyUser(authorizable).purge();
    }

    private AclResult apply(
            Object authorizableObj,
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            PermissionsOptions.Mode mode,
            boolean allow) {
        Authorizable authorizable = determineAuthorizable(authorizableObj);
        if (allow) {
            return forMyAuthorizable(authorizable)
                    .allow(path, permissions, glob, types, properties, restrictions, null);
        } else {
            return forMyAuthorizable(authorizable).deny(path, permissions, glob, types, properties, restrictions, null);
        }
    }

    public AclResult allow(PermissionsOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        return apply(
                authorizable,
                options.getPath(),
                options.getPermissions(),
                options.getGlob(),
                options.getTypes(),
                options.getProperties(),
                options.getRestrictions(),
                options.getMode(),
                true);
    }

    public AclResult allow(
            Object authorizableObj,
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            PermissionsOptions.Mode mode) {
        return apply(authorizableObj, path, permissions, glob, types, properties, restrictions, mode, true);
    }

    public AclResult allow(Object authorizableObj, String path, List<String> permissions) {
        return apply(authorizableObj, path, permissions, null, null, null, null, null, true);
    }

    public AclResult allow(Object authorizableObj, String path, List<String> permissions, String glob) {
        return apply(authorizableObj, path, permissions, glob, null, null, null, null, true);
    }

    public AclResult allow(
            Object authorizableObj, String path, List<String> permissions, Map<String, Object> restrictions) {
        return apply(authorizableObj, path, permissions, null, null, null, restrictions, null, true);
    }

    public AclResult deny(PermissionsOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        return apply(
                authorizable,
                options.getPath(),
                options.getPermissions(),
                options.getGlob(),
                options.getTypes(),
                options.getProperties(),
                options.getRestrictions(),
                options.getMode(),
                false);
    }

    public AclResult deny(
            Object authorizableObj,
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            PermissionsOptions.Mode mode) {
        return apply(authorizableObj, path, permissions, glob, types, properties, restrictions, mode, false);
    }

    public AclResult deny(Object authorizableObj, String path, List<String> permissions) {
        return apply(authorizableObj, path, permissions, null, null, null, null, null, false);
    }

    public AclResult deny(Object authorizableObj, String path, List<String> permissions, String glob) {
        return apply(authorizableObj, path, permissions, glob, null, null, null, null, false);
    }

    public AclResult deny(
            Object authorizableObj, String path, List<String> permissions, Map<String, Object> restrictions) {
        return apply(authorizableObj, path, permissions, null, null, null, restrictions, null, false);
    }

    public AclResult setProperty(SetPropertyOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        return setProperty(authorizable, options.getRelPath(), options.getValue());
    }

    public AclResult setProperty(Object authorizableObj, String relPath, String value) {
        Authorizable authorizable = determineAuthorizable(authorizableObj);
        return forMyAuthorizable(authorizable).setProperty(relPath, value);
    }

    public AclResult removeProperty(RemovePropertyOptions options) {
        Authorizable authorizable = determineAuthorizable(options);
        return removeProperty(authorizable, options.getRelPath());
    }

    public AclResult removeProperty(Object authorizableObj, String relPath) {
        Authorizable authorizable = determineAuthorizable(authorizableObj);
        return forMyAuthorizable(authorizable).removeProperty(relPath);
    }

    public AclResult setPassword(PasswordOptions options) {
        Authorizable user = determineAuthorizable(options);
        return setPassword(user, options.getPassword());
    }

    public AclResult setPassword(Object userObj, String password) {
        Authorizable user = determineAuthorizable(userObj);
        return forMyUser(user).setPassword(password);
    }

    private Authorizable determineAuthorizable(Object authorizableObj) {
        return authorizableManager.determineAuthorizable(authorizableObj);
    }

    private Authorizable determineAuthorizable(Object authorizableObj, String id) {
        return authorizableManager.determineAuthorizable(authorizableObj, id);
    }

    private MyAuthorizable forMyAuthorizable(Authorizable authorizable) {
        return new MyAuthorizable(
                authorizable, resourceResolver, authorizableManager, permissionsManager, compositeNodeStore, out);
    }

    private MyUser forMyUser(Authorizable authorizable) {
        return new MyUser(
                authorizable, resourceResolver, authorizableManager, permissionsManager, compositeNodeStore, out);
    }

    private MyGroup forMyGroup(Authorizable authorizable) {
        return new MyGroup(
                authorizable, resourceResolver, authorizableManager, permissionsManager, compositeNodeStore, out);
    }

    private boolean notExists(Authorizable authorizable) {
        return authorizable == null || authorizable instanceof UnknownAuthorizable;
    }

    protected String getID(Authorizable authorizable) {
        try {
            return authorizable.getID();
        } catch (RepositoryException e) {
            return "";
        }
    }

    private void logResult(Authorizable authorizable, String messagePattern, Object... args) {
        try {
            logResult(authorizable.getID(), messagePattern, args);
        } catch (RepositoryException e) {
            throw new AclException("Failed to get authorizable ID", e);
        }
    }

    private void logResult(String id, String messagePattern, Object... args) {
        try {
            String newMessagePattern = String.format("[%s] %s\n", id, messagePattern);
            String message = MessageFormatter.format(newMessagePattern, args).getMessage();
            out.write(message.getBytes());
        } catch (IOException e) {
            throw new AclException("Failed to log message", e);
        }
    }
}
