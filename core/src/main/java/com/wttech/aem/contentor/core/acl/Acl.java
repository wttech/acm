package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.MyAuthorizable;
import com.wttech.aem.contentor.core.acl.authorizable.MyGroup;
import com.wttech.aem.contentor.core.acl.authorizable.MyUser;
import com.wttech.aem.contentor.core.acl.check.CheckAcl;
import com.wttech.aem.contentor.core.util.GroovyUtils;
import groovy.lang.Closure;
import java.util.List;
import java.util.Map;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.resource.ResourceResolver;

public class Acl {

    private final AclContext context;

    public final CheckAcl check;

    public Acl(ResourceResolver resourceResolver) {
        this.context = new AclContext(resourceResolver);
        this.check = new CheckAcl(context);
    }

    public MyUser createUser(Closure<CreateUserOptions> closure) {
        return createUser(GroovyUtils.with(new CreateUserOptions(), closure));
    }

    public MyGroup createGroup(Closure<CreateGroupOptions> closure) {
        return createGroup(GroovyUtils.with(new CreateGroupOptions(), closure));
    }

    public MyUser getUser(Closure<GetAuthorizableOptions> closure) {
        return getUser(GroovyUtils.with(new GetAuthorizableOptions(), closure));
    }

    public MyGroup getGroup(Closure<GetAuthorizableOptions> closure) {
        return getGroup(GroovyUtils.with(new GetAuthorizableOptions(), closure));
    }

    public AclResult deleteUser(Closure<DeleteUserOptions> closure) {
        return deleteUser(GroovyUtils.with(new DeleteUserOptions(), closure));
    }

    public AclResult deleteGroup(Closure<DeleteGroupOptions> closure) {
        return deleteGroup(GroovyUtils.with(new DeleteGroupOptions(), closure));
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

    public AclResult removeAllMembers(Closure<RemoveAllMembersOptions> closure) {
        return removeAllMembers(GroovyUtils.with(new RemoveAllMembersOptions(), closure));
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

    public MyUser createUser(
            String id,
            String password,
            String path,
            String givenName,
            String familyName,
            String email,
            String aboutMe,
            boolean systemUser,
            CreateAuthorizableOptions.Mode mode) {
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
        User user = context.getAuthorizableManager().getUser(options.getId());
        if (user == null) {
            if (options.isSystemUser()) {
                user = context.getAuthorizableManager().createSystemUser(options.getId(), options.getPath());
            } else {
                user = context.getAuthorizableManager()
                        .createUser(options.getId(), options.getPassword(), options.getPath());
            }
            context.getAuthorizableManager().updateUser(user, options.getPassword(), options.determineProperties());
        } else if (options.getMode() == CreateAuthorizableOptions.Mode.FAIL) {
            throw new AclException(String.format("User with id %s already exists", options.getId()));
        } else if (options.getMode() == CreateAuthorizableOptions.Mode.OVERRIDE) {
            context.getAuthorizableManager().updateUser(user, options.getPassword(), options.determineProperties());
        }
        MyUser myUser = context.determineUser(user);
        context.logResult(myUser, "createUser");
        return myUser;
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
            CreateAuthorizableOptions.Mode mode) {
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
        Group group = context.getAuthorizableManager().getGroup(options.getId());
        if (group == null) {
            group = context.getAuthorizableManager()
                    .createGroup(options.getId(), options.getPath(), options.getExternalId());
            context.getAuthorizableManager().updateGroup(group, options.determineProperties());
        } else if (options.getMode() == CreateAuthorizableOptions.Mode.FAIL) {
            throw new AclException(String.format("Group with id %s already exists", options.getId()));
        } else if (options.getMode() == CreateAuthorizableOptions.Mode.OVERRIDE) {
            context.getAuthorizableManager().updateGroup(group, options.determineProperties());
        }
        MyGroup myGroup = context.determineGroup(group);
        context.logResult(myGroup, "createGroup");
        return myGroup;
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

    public MyUser getUser(GetAuthorizableOptions options) {
        return getUser(options.getId());
    }

    public MyUser getUser(String id) {
        MyUser user = context.determineUser(id);
        AclResult result = user.get() == null ? AclResult.SKIPPED : AclResult.OK;
        context.logResult(user, "getUser {}", result);
        return result == AclResult.OK ? user : null;
    }

    public MyGroup getGroup(GetAuthorizableOptions options) {
        return getGroup(options.getId());
    }

    public MyGroup getGroup(String id) {
        MyGroup group = context.determineGroup(id);
        AclResult result = group.get() == null ? AclResult.SKIPPED : AclResult.OK;
        context.logResult(group, "getGroup {}", result);
        return result == AclResult.OK ? group : null;
    }

    public AclResult deleteUser(DeleteUserOptions options) {
        MyUser user = context.determineUser(options.getUser(), options.getId());
        return deleteUser(user);
    }

    public AclResult deleteUser(String id) {
        MyUser user = context.determineUser(id);
        return deleteUser(user);
    }

    public AclResult deleteUser(MyUser user) {
        AclResult result;
        if (user.get() == null) {
            result = AclResult.OK;
        } else {
            purge(user);
            context.getAuthorizableManager().deleteAuthorizable(user.get());
            result = AclResult.CHANGED;
        }
        context.logResult(user, "deleteUser {}", result);
        return result;
    }

    public AclResult deleteGroup(DeleteGroupOptions options) {
        MyGroup group = context.determineGroup(options.getGroup(), options.getId());
        return deleteGroup(group);
    }

    public AclResult deleteGroup(String id) {
        MyGroup group = context.determineGroup(id);
        return deleteGroup(group);
    }

    public AclResult deleteGroup(MyGroup group) {
        AclResult result;
        if (group.get() == null) {
            result = AclResult.OK;
        } else {
            purge(group);
            context.getAuthorizableManager().deleteAuthorizable(group.get());
            result = AclResult.CHANGED;
        }
        context.logResult(group, "deleteGroup {}", result);
        return result;
    }

    public AclResult addToGroup(GroupOptions options) {
        MyAuthorizable authorizable = context.determineAuthorizable(options);
        MyGroup group = context.determineGroup(options.getGroup(), options.getGroupId());
        return authorizable.addToGroup(group);
    }

    public AclResult addToGroup(String authorizableId, String groupId) {
        MyAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        MyGroup group = context.determineGroup(groupId);
        return authorizable.addToGroup(group);
    }

    public AclResult addToGroup(String authorizableId, MyGroup group) {
        MyAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.addToGroup(group);
    }

    public AclResult addToGroup(MyAuthorizable authorizable, String groupId) {
        MyGroup group = context.determineGroup(groupId);
        return authorizable.addToGroup(group);
    }

    public AclResult addToGroup(MyAuthorizable authorizable, MyGroup group) {
        return authorizable.addToGroup(group);
    }

    public AclResult removeFromGroup(GroupOptions options) {
        MyAuthorizable authorizable = context.determineAuthorizable(options);
        MyGroup group = context.determineGroup(options.getGroup(), options.getGroupId());
        return authorizable.removeFromGroup(group);
    }

    public AclResult removeFromGroup(String authorizableId, String groupId) {
        MyAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        MyGroup group = context.determineGroup(groupId);
        return authorizable.removeFromGroup(group);
    }

    public AclResult removeFromGroup(MyAuthorizable authorizable, MyGroup group) {
        return authorizable.removeFromGroup(group);
    }

    public AclResult removeFromAllGroups(AuthorizableOptions options) {
        MyAuthorizable authorizable = context.determineAuthorizable(options);
        return authorizable.removeFromAllGroups();
    }

    public AclResult removeFromAllGroups(String authorizableId) {
        MyAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.removeFromAllGroups();
    }

    public AclResult removeFromAllGroups(MyAuthorizable authorizable) {
        return authorizable.removeFromAllGroups();
    }

    public AclResult addMember(MemberOptions options) {
        MyGroup group = context.determineGroup(options.getGroup(), options.getGroupId());
        MyAuthorizable member = context.determineAuthorizable(options.getMember(), options.getMemberId());
        return group.addMember(member);
    }

    public AclResult addMember(String groupId, String memberId) {
        MyGroup group = context.determineGroup(groupId);
        return group.addMember(memberId);
    }

    public AclResult addMember(String groupId, MyAuthorizable member) {
        MyGroup group = context.determineGroup(groupId);
        return group.addMember(member);
    }

    public AclResult addMember(MyGroup group, String memberId) {
        return group.addMember(memberId);
    }

    public AclResult addMember(MyGroup group, MyAuthorizable member) {
        return group.addMember(member);
    }

    public AclResult removeMember(MemberOptions options) {
        MyGroup group = context.determineGroup(options.getGroup(), options.getGroupId());
        MyAuthorizable member = context.determineAuthorizable(options.getMember(), options.getMemberId());
        return group.removeMember(member);
    }

    public AclResult removeMember(String groupId, String memberId) {
        MyGroup group = context.determineGroup(groupId);
        return group.removeMember(memberId);
    }

    public AclResult removeMember(MyGroup group, MyAuthorizable member) {
        return group.removeMember(member);
    }

    public AclResult removeAllMembers(RemoveAllMembersOptions options) {
        MyGroup group = context.determineGroup(options.getGroup(), options.getGroupId());
        return group.removeAllMembers();
    }

    public AclResult removeAllMembers(String groupId) {
        MyGroup group = context.determineGroup(groupId);
        return group.removeAllMembers();
    }

    public AclResult removeAllMembers(MyGroup group) {
        return group.removeAllMembers();
    }

    public AclResult clear(ClearOptions options) {
        MyAuthorizable authorizable = context.determineAuthorizable(options);
        return authorizable.clear(options.getPath(), options.isStrict());
    }

    public AclResult clear(String authorizableId, String path, boolean strict) {
        MyAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.clear(path, strict);
    }

    public AclResult clear(String authorizableId, String path) {
        MyAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.clear(path);
    }

    public AclResult clear(MyAuthorizable authorizable, String path, boolean strict) {
        return authorizable.clear(path, strict);
    }

    public AclResult clear(MyAuthorizable authorizable, String path) {
        return authorizable.clear(path);
    }

    public AclResult purge(AuthorizableOptions options) {
        MyAuthorizable authorizable = context.determineAuthorizable(options);
        return authorizable.purge();
    }

    public AclResult purge(String authorizableId) {
        MyAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.purge();
    }

    public AclResult purge(MyAuthorizable authorizable) {
        return authorizable.purge();
    }

    public AclResult allow(PermissionsOptions options) {
        MyAuthorizable authorizable = context.determineAuthorizable(options);
        return authorizable.allow(
                options.getPath(),
                options.getPermissions(),
                options.getGlob(),
                options.getTypes(),
                options.getProperties(),
                options.getRestrictions(),
                null);
    }

    public AclResult allow(
            String authorizableId,
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            PermissionsOptions.Mode mode) {
        MyAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.allow(
                path, permissions, glob, types, properties, restrictions, PermissionsOptions.Mode.cast(mode));
    }

    public AclResult allow(
            MyAuthorizable authorizable,
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            PermissionsOptions.Mode mode) {
        return authorizable.allow(
                path, permissions, glob, types, properties, restrictions, PermissionsOptions.Mode.cast(mode));
    }

    public AclResult allow(String authorizableId, String path, List<String> permissions) {
        MyAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.allow(path, permissions);
    }

    public AclResult allow(MyAuthorizable authorizable, String path, List<String> permissions) {
        return authorizable.allow(path, permissions);
    }

    public AclResult allow(String authorizableId, String path, List<String> permissions, String glob) {
        MyAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.allow(path, permissions, glob);
    }

    public AclResult allow(MyAuthorizable authorizable, String path, List<String> permissions, String glob) {
        return authorizable.allow(path, permissions, glob);
    }

    public AclResult allow(
            String authorizableId, String path, List<String> permissions, Map<String, Object> restrictions) {

        MyAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.allow(path, permissions, restrictions);
    }

    public AclResult allow(
            MyAuthorizable authorizable, String path, List<String> permissions, Map<String, Object> restrictions) {
        return authorizable.allow(path, permissions, restrictions);
    }

    public AclResult deny(PermissionsOptions options) {
        MyAuthorizable authorizable = context.determineAuthorizable(options);
        return authorizable.deny(
                options.getPath(),
                options.getPermissions(),
                options.getGlob(),
                options.getTypes(),
                options.getProperties(),
                options.getRestrictions(),
                null);
    }

    public AclResult deny(
            String authorizableId,
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            PermissionsOptions.Mode mode) {
        MyAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.deny(
                path, permissions, glob, types, properties, restrictions, PermissionsOptions.Mode.cast(mode));
    }

    public AclResult deny(
            MyAuthorizable authorizable,
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            PermissionsOptions.Mode mode) {
        return authorizable.deny(
                path, permissions, glob, types, properties, restrictions, PermissionsOptions.Mode.cast(mode));
    }

    public AclResult deny(String authorizableId, String path, List<String> permissions) {
        MyAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.deny(path, permissions);
    }

    public AclResult deny(MyAuthorizable authorizable, String path, List<String> permissions) {
        return authorizable.deny(path, permissions);
    }

    public AclResult deny(String authorizableId, String path, List<String> permissions, String glob) {
        MyAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.deny(path, permissions, glob);
    }

    public AclResult deny(MyAuthorizable authorizable, String path, List<String> permissions, String glob) {
        return authorizable.deny(path, permissions, glob);
    }

    public AclResult deny(
            String authorizableId, String path, List<String> permissions, Map<String, Object> restrictions) {
        MyAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.deny(path, permissions, restrictions);
    }

    public AclResult deny(
            MyAuthorizable authorizable, String path, List<String> permissions, Map<String, Object> restrictions) {
        return authorizable.deny(path, permissions, restrictions);
    }

    public AclResult setProperty(SetPropertyOptions options) {
        MyAuthorizable authorizable = context.determineAuthorizable(options);
        return authorizable.setProperty(options.getRelPath(), options.getValue());
    }

    public AclResult setProperty(String authorizableId, String relPath, String value) {
        MyAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.setProperty(relPath, value);
    }

    public AclResult setProperty(MyAuthorizable authorizable, String relPath, String value) {
        return authorizable.setProperty(relPath, value);
    }

    public AclResult removeProperty(RemovePropertyOptions options) {
        MyAuthorizable authorizable = context.determineAuthorizable(options);
        return authorizable.removeProperty(options.getRelPath());
    }

    public AclResult removeProperty(String authorizableId, String relPath) {
        MyAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.removeProperty(relPath);
    }

    public AclResult removeProperty(MyAuthorizable authorizable, String relPath) {
        return authorizable.removeProperty(relPath);
    }

    public AclResult setPassword(PasswordOptions options) {
        MyUser user = context.determineUser(options.getUser(), options.getUserId());
        return user.setPassword(options.getPassword());
    }

    public AclResult setPassword(String userId, String password) {
        MyUser user = context.determineUser(userId);
        return user.setPassword(password);
    }

    public AclResult setPassword(MyUser user, String password) {
        return user.setPassword(password);
    }

    public MyUser user(User user) {
        return user == null ? null : context.determineUser(user);
    }

    public MyGroup group(Group group) {
        return group == null ? null : context.determineGroup(group);
    }
}
