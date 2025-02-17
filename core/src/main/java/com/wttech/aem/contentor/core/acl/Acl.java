package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclAuthorizable;
import com.wttech.aem.contentor.core.acl.authorizable.AclGroup;
import com.wttech.aem.contentor.core.acl.authorizable.AclUser;
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

    public AclUser createUser(Closure<CreateUserOptions> closure) {
        return createUser(GroovyUtils.with(new CreateUserOptions(), closure));
    }

    public AclGroup createGroup(Closure<CreateGroupOptions> closure) {
        return createGroup(GroovyUtils.with(new CreateGroupOptions(), closure));
    }

    public AclUser getUser(Closure<GetAuthorizableOptions> closure) {
        return getUser(GroovyUtils.with(new GetAuthorizableOptions(), closure));
    }

    public AclGroup getGroup(Closure<GetAuthorizableOptions> closure) {
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

    public AclUser createUser(
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

    public AclUser createUser(CreateUserOptions options) {
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
        AclUser myUser = context.determineUser(user);
        context.logger.info("Created user '{}'", myUser);
        return myUser;
    }

    public AclUser createUser(String id) {
        CreateUserOptions options = new CreateUserOptions();
        options.setId(id);
        return createUser(options);
    }

    public AclUser createSystemUser(String id) {
        CreateUserOptions options = new CreateUserOptions();
        options.setId(id);
        options.systemUser();
        return createUser(options);
    }

    public AclGroup createGroup(
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

    public AclGroup createGroup(CreateGroupOptions options) {
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
        AclGroup aclGroup = context.determineGroup(group);
        context.getLogger().info("Created group '{}'", aclGroup);
        return aclGroup;
    }

    public AclGroup createGroup(String id) {
        CreateGroupOptions options = new CreateGroupOptions();
        options.setId(id);
        return createGroup(options);
    }

    public AclGroup createGroup(String id, String externalId) {
        CreateGroupOptions options = new CreateGroupOptions();
        options.setId(id);
        options.setExternalId(externalId);
        return createGroup(options);
    }

    public AclUser getUser(GetAuthorizableOptions options) {
        return getUser(options.getId());
    }

    public AclUser getUser(String id) {
        AclUser user = context.determineUser(id);
        return user.get() != null ? user : null;
    }

    public AclGroup getGroup(GetAuthorizableOptions options) {
        return getGroup(options.getId());
    }

    public AclGroup getGroup(String id) {
        AclGroup group = context.determineGroup(id);
        return group.get() != null ? group : null;
    }

    public AclResult deleteUser(DeleteUserOptions options) {
        AclUser user = context.determineUser(options.getUser(), options.getId());
        return deleteUser(user);
    }

    public AclResult deleteUser(String id) {
        AclUser user = context.determineUser(id);
        return deleteUser(user);
    }

    public AclResult deleteUser(AclUser user) {
        AclResult result;
        if (user.get() == null) {
            result = AclResult.OK;
        } else {
            purge(user);
            context.getAuthorizableManager().deleteAuthorizable(user.get());
            result = AclResult.CHANGED;
        }
        context.getLogger().info("Deleted user '{}' [{}]", user.getId(), result);
        return result;
    }

    public AclResult deleteGroup(DeleteGroupOptions options) {
        AclGroup group = context.determineGroup(options.getGroup(), options.getId());
        return deleteGroup(group);
    }

    public AclResult deleteGroup(String id) {
        AclGroup group = context.determineGroup(id);
        return deleteGroup(group);
    }

    public AclResult deleteGroup(AclGroup group) {
        AclResult result;
        if (group.get() == null) {
            result = AclResult.OK;
        } else {
            purge(group);
            context.getAuthorizableManager().deleteAuthorizable(group.get());
            result = AclResult.CHANGED;
        }
        context.getLogger().info("Deleted group '{}' [{}]", group.getId(), result);
        return result;
    }

    public AclResult addToGroup(GroupOptions options) {
        AclAuthorizable authorizable = context.determineAuthorizable(options);
        AclGroup group = context.determineGroup(options.getGroup(), options.getGroupId());
        return authorizable.addToGroup(group);
    }

    public AclResult addToGroup(String authorizableId, String groupId) {
        AclAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        AclGroup group = context.determineGroup(groupId);
        return authorizable.addToGroup(group);
    }

    public AclResult addToGroup(String authorizableId, AclGroup group) {
        AclAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.addToGroup(group);
    }

    public AclResult addToGroup(AclAuthorizable authorizable, String groupId) {
        AclGroup group = context.determineGroup(groupId);
        return authorizable.addToGroup(group);
    }

    public AclResult addToGroup(AclAuthorizable authorizable, AclGroup group) {
        return authorizable.addToGroup(group);
    }

    public AclResult removeFromGroup(GroupOptions options) {
        AclAuthorizable authorizable = context.determineAuthorizable(options);
        AclGroup group = context.determineGroup(options.getGroup(), options.getGroupId());
        return authorizable.removeFromGroup(group);
    }

    public AclResult removeFromGroup(String authorizableId, String groupId) {
        AclAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        AclGroup group = context.determineGroup(groupId);
        return authorizable.removeFromGroup(group);
    }

    public AclResult removeFromGroup(AclAuthorizable authorizable, AclGroup group) {
        return authorizable.removeFromGroup(group);
    }

    public AclResult removeFromAllGroups(AuthorizableOptions options) {
        AclAuthorizable authorizable = context.determineAuthorizable(options);
        return authorizable.removeFromAllGroups();
    }

    public AclResult removeFromAllGroups(String authorizableId) {
        AclAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.removeFromAllGroups();
    }

    public AclResult removeFromAllGroups(AclAuthorizable authorizable) {
        return authorizable.removeFromAllGroups();
    }

    public AclResult addMember(MemberOptions options) {
        AclGroup group = context.determineGroup(options.getGroup(), options.getGroupId());
        AclAuthorizable member = context.determineAuthorizable(options.getMember(), options.getMemberId());
        return group.addMember(member);
    }

    public AclResult addMember(String groupId, String memberId) {
        AclGroup group = context.determineGroup(groupId);
        return group.addMember(memberId);
    }

    public AclResult addMember(String groupId, AclAuthorizable member) {
        AclGroup group = context.determineGroup(groupId);
        return group.addMember(member);
    }

    public AclResult addMember(AclGroup group, String memberId) {
        return group.addMember(memberId);
    }

    public AclResult addMember(AclGroup group, AclAuthorizable member) {
        return group.addMember(member);
    }

    public AclResult removeMember(MemberOptions options) {
        AclGroup group = context.determineGroup(options.getGroup(), options.getGroupId());
        AclAuthorizable member = context.determineAuthorizable(options.getMember(), options.getMemberId());
        return group.removeMember(member);
    }

    public AclResult removeMember(String groupId, String memberId) {
        AclGroup group = context.determineGroup(groupId);
        return group.removeMember(memberId);
    }

    public AclResult removeMember(AclGroup group, AclAuthorizable member) {
        return group.removeMember(member);
    }

    public AclResult removeAllMembers(RemoveAllMembersOptions options) {
        AclGroup group = context.determineGroup(options.getGroup(), options.getGroupId());
        return group.removeAllMembers();
    }

    public AclResult removeAllMembers(String groupId) {
        AclGroup group = context.determineGroup(groupId);
        return group.removeAllMembers();
    }

    public AclResult removeAllMembers(AclGroup group) {
        return group.removeAllMembers();
    }

    public AclResult clear(ClearOptions options) {
        AclAuthorizable authorizable = context.determineAuthorizable(options);
        return authorizable.clear(options.getPath(), options.isStrict());
    }

    public AclResult clear(String authorizableId, String path, boolean strict) {
        AclAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.clear(path, strict);
    }

    public AclResult clear(String authorizableId, String path) {
        AclAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.clear(path);
    }

    public AclResult clear(AclAuthorizable authorizable, String path, boolean strict) {
        return authorizable.clear(path, strict);
    }

    public AclResult clear(AclAuthorizable authorizable, String path) {
        return authorizable.clear(path);
    }

    public AclResult purge(AuthorizableOptions options) {
        AclAuthorizable authorizable = context.determineAuthorizable(options);
        return authorizable.purge();
    }

    public AclResult purge(String authorizableId) {
        AclAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.purge();
    }

    public AclResult purge(AclAuthorizable authorizable) {
        return authorizable.purge();
    }

    public AclResult allow(PermissionsOptions options) {
        AclAuthorizable authorizable = context.determineAuthorizable(options);
        return authorizable.allow(
                options.getPath(),
                options.getPermissions(),
                options.getGlob(),
                options.getTypes(),
                options.getProperties(),
                options.getRestrictions(),
                PermissionsOptions.Mode.cast(options.getMode()));
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
        AclAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.allow(
                path, permissions, glob, types, properties, restrictions, PermissionsOptions.Mode.cast(mode));
    }

    public AclResult allow(
            AclAuthorizable authorizable,
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
        AclAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.allow(path, permissions);
    }

    public AclResult allow(AclAuthorizable authorizable, String path, List<String> permissions) {
        return authorizable.allow(path, permissions);
    }

    public AclResult allow(String authorizableId, String path, List<String> permissions, String glob) {
        AclAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.allow(path, permissions, glob);
    }

    public AclResult allow(AclAuthorizable authorizable, String path, List<String> permissions, String glob) {
        return authorizable.allow(path, permissions, glob);
    }

    public AclResult allow(
            String authorizableId, String path, List<String> permissions, Map<String, Object> restrictions) {

        AclAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.allow(path, permissions, restrictions);
    }

    public AclResult allow(
            AclAuthorizable authorizable, String path, List<String> permissions, Map<String, Object> restrictions) {
        return authorizable.allow(path, permissions, restrictions);
    }

    public AclResult deny(PermissionsOptions options) {
        AclAuthorizable authorizable = context.determineAuthorizable(options);
        return authorizable.deny(
                options.getPath(),
                options.getPermissions(),
                options.getGlob(),
                options.getTypes(),
                options.getProperties(),
                options.getRestrictions(),
                PermissionsOptions.Mode.cast(options.getMode()));
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
        AclAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.deny(
                path, permissions, glob, types, properties, restrictions, PermissionsOptions.Mode.cast(mode));
    }

    public AclResult deny(
            AclAuthorizable authorizable,
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
        AclAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.deny(path, permissions);
    }

    public AclResult deny(AclAuthorizable authorizable, String path, List<String> permissions) {
        return authorizable.deny(path, permissions);
    }

    public AclResult deny(String authorizableId, String path, List<String> permissions, String glob) {
        AclAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.deny(path, permissions, glob);
    }

    public AclResult deny(AclAuthorizable authorizable, String path, List<String> permissions, String glob) {
        return authorizable.deny(path, permissions, glob);
    }

    public AclResult deny(
            String authorizableId, String path, List<String> permissions, Map<String, Object> restrictions) {
        AclAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.deny(path, permissions, restrictions);
    }

    public AclResult deny(
            AclAuthorizable authorizable, String path, List<String> permissions, Map<String, Object> restrictions) {
        return authorizable.deny(path, permissions, restrictions);
    }

    public AclResult setProperty(SetPropertyOptions options) {
        AclAuthorizable authorizable = context.determineAuthorizable(options);
        return authorizable.setProperty(options.getRelPath(), options.getValue());
    }

    public AclResult setProperty(String authorizableId, String relPath, String value) {
        AclAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.setProperty(relPath, value);
    }

    public AclResult setProperty(AclAuthorizable authorizable, String relPath, String value) {
        return authorizable.setProperty(relPath, value);
    }

    public AclResult removeProperty(RemovePropertyOptions options) {
        AclAuthorizable authorizable = context.determineAuthorizable(options);
        return authorizable.removeProperty(options.getRelPath());
    }

    public AclResult removeProperty(String authorizableId, String relPath) {
        AclAuthorizable authorizable = context.determineAuthorizable(authorizableId);
        return authorizable.removeProperty(relPath);
    }

    public AclResult removeProperty(AclAuthorizable authorizable, String relPath) {
        return authorizable.removeProperty(relPath);
    }

    public AclResult setPassword(PasswordOptions options) {
        AclUser user = context.determineUser(options.getUser(), options.getUserId());
        return user.setPassword(options.getPassword());
    }

    public AclResult setPassword(String userId, String password) {
        AclUser user = context.determineUser(userId);
        return user.setPassword(password);
    }

    public AclResult setPassword(AclUser user, String password) {
        return user.setPassword(password);
    }

    public AclUser user(User user) {
        return user == null ? null : context.determineUser(user);
    }

    public AclGroup group(Group group) {
        return group == null ? null : context.determineGroup(group);
    }
}
