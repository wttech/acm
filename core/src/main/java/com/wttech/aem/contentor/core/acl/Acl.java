package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.AclAuthorizable;
import com.wttech.aem.contentor.core.acl.authorizable.AclGroup;
import com.wttech.aem.contentor.core.acl.authorizable.AclUser;
import com.wttech.aem.contentor.core.acl.authorizable.PermissionsMode;
import com.wttech.aem.contentor.core.acl.check.CheckAcl;
import com.wttech.aem.contentor.core.util.GroovyUtils;
import groovy.lang.Closure;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        AclResult result = AclResult.OK;
        if (user == null) {
            if (options.isSystemUser()) {
                user = context.getAuthorizableManager().createSystemUser(options.getId(), options.getPath());
            } else {
                user = context.getAuthorizableManager()
                        .createUser(options.getId(), options.getPassword(), options.getPath());
            }
            context.getAuthorizableManager().updateUser(user, options.getPassword(), options.determineProperties());
            result = AclResult.CHANGED;
        } else if (options.getMode() == CreateAuthorizableOptions.Mode.FAIL) {
            throw new AclException(String.format("User '%s' already exists", options.getId()));
        } else if (options.getMode() == CreateAuthorizableOptions.Mode.OVERRIDE) {
            context.getAuthorizableManager().updateUser(user, options.getPassword(), options.determineProperties());
            result = AclResult.CHANGED;
        }
        AclUser aclUser = context.determineUser(user);
        context.getLogger().info("Created user '{}' [{}]", aclUser, result);
        return aclUser;
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
        AclResult result = AclResult.OK;
        if (group == null) {
            group = context.getAuthorizableManager()
                    .createGroup(options.getId(), options.getPath(), options.getExternalId());
            context.getAuthorizableManager().updateGroup(group, options.determineProperties());
            result = AclResult.CHANGED;
        } else if (options.getMode() == CreateAuthorizableOptions.Mode.FAIL) {
            throw new AclException(String.format("Group '%s' already exists", options.getId()));
        } else if (options.getMode() == CreateAuthorizableOptions.Mode.OVERRIDE) {
            context.getAuthorizableManager().updateGroup(group, options.determineProperties());
            result = AclResult.CHANGED;
        }
        AclGroup aclGroup = context.determineGroup(group);
        context.getLogger().info("Created group '{}' [{}]", aclGroup, result);
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
        return context.determineUser(options.getId());
    }

    public AclUser getUser(String id) {
        GetAuthorizableOptions options = new GetAuthorizableOptions();
        options.setId(id);
        return getUser(options);
    }

    public AclGroup getGroup(GetAuthorizableOptions options) {
        return context.determineGroup(options.getId());
    }

    public AclGroup getGroup(String id) {
        GetAuthorizableOptions options = new GetAuthorizableOptions();
        options.setId(id);
        return getGroup(options);
    }

    public AclResult deleteUser(DeleteUserOptions options) {
        AclUser user = Optional.ofNullable(options.getUser()).orElse(context.determineUser(options.getId()));
        String id = Optional.ofNullable(options.getUser())
                .map(AclAuthorizable::getId)
                .orElse(options.getId());
        AclResult result;
        if (user == null) {
            result = AclResult.OK;
        } else if (context.getAuthorizableManager().getAuthorizable(id) == null) {
            result = AclResult.OK;
        } else {
            purge(user);
            context.getAuthorizableManager().deleteAuthorizable(user.get());
            result = AclResult.CHANGED;
        }
        context.getLogger().info("Deleted user '{}' [{}]", id, result);
        return result;
    }

    public AclResult deleteUser(String id) {
        DeleteUserOptions options = new DeleteUserOptions();
        options.setId(id);
        return deleteUser(options);
    }

    public AclResult deleteUser(AclUser user) {
        DeleteUserOptions options = new DeleteUserOptions();
        options.setUser(user);
        return deleteUser(options);
    }

    public AclResult deleteGroup(DeleteGroupOptions options) {
        AclGroup group = Optional.ofNullable(options.getGroup()).orElse(context.determineGroup(options.getId()));
        String id = Optional.ofNullable(options.getGroup())
                .map(AclAuthorizable::getId)
                .orElse(options.getId());
        AclResult result;
        if (group == null) {
            result = AclResult.OK;
        } else if (context.getAuthorizableManager().getAuthorizable(id) == null) {
            result = AclResult.OK;
        } else {
            purge(group);
            context.getAuthorizableManager().deleteAuthorizable(group.get());
            result = AclResult.CHANGED;
        }
        context.getLogger().info("Deleted group '{}' [{}]", id, result);
        return result;
    }

    public AclResult deleteGroup(String id) {
        DeleteGroupOptions options = new DeleteGroupOptions();
        options.setId(id);
        return deleteGroup(options);
    }

    public AclResult deleteGroup(AclGroup group) {
        DeleteGroupOptions options = new DeleteGroupOptions();
        options.setGroup(group);
        return deleteGroup(options);
    }

    public AclResult addToGroup(GroupOptions options) {
        AclAuthorizable authorizable = Optional.ofNullable(options.getAuthorizable())
                .orElse(context.determineAuthorizable(options.getAuthorizableId()));
        return authorizable.addToGroup(options);
    }

    public AclResult addToGroup(String authorizableId, String groupId) {
        GroupOptions options = new GroupOptions();
        options.setAuthorizableId(authorizableId);
        options.setGroupId(groupId);
        return addToGroup(options);
    }

    public AclResult addToGroup(String authorizableId, AclGroup group) {
        GroupOptions options = new GroupOptions();
        options.setAuthorizableId(authorizableId);
        options.setGroup(group);
        return addToGroup(options);
    }

    public AclResult addToGroup(AclAuthorizable authorizable, String groupId) {
        GroupOptions options = new GroupOptions();
        options.setAuthorizable(authorizable);
        options.setGroupId(groupId);
        return addToGroup(options);
    }

    public AclResult addToGroup(AclAuthorizable authorizable, AclGroup group) {
        GroupOptions options = new GroupOptions();
        options.setAuthorizable(authorizable);
        options.setGroup(group);
        return addToGroup(options);
    }

    public AclResult removeFromGroup(GroupOptions options) {
        AclAuthorizable authorizable = Optional.ofNullable(options.getAuthorizable())
                .orElse(context.determineAuthorizable(options.getAuthorizableId()));
        return authorizable.removeFromGroup(options);
    }

    public AclResult removeFromGroup(String authorizableId, String groupId) {
        GroupOptions options = new GroupOptions();
        options.setAuthorizableId(authorizableId);
        options.setGroupId(groupId);
        return removeFromGroup(options);
    }

    public AclResult removeFromGroup(String authorizableId, AclGroup group) {
        GroupOptions options = new GroupOptions();
        options.setAuthorizableId(authorizableId);
        options.setGroup(group);
        return removeFromGroup(options);
    }

    public AclResult removeFromGroup(AclAuthorizable authorizable, String groupId) {
        GroupOptions options = new GroupOptions();
        options.setAuthorizable(authorizable);
        options.setGroupId(groupId);
        return removeFromGroup(options);
    }

    public AclResult removeFromGroup(AclAuthorizable authorizable, AclGroup group) {
        GroupOptions options = new GroupOptions();
        options.setAuthorizable(authorizable);
        options.setGroup(group);
        return removeFromGroup(options);
    }

    public AclResult removeFromAllGroups(AuthorizableOptions options) {
        AclAuthorizable authorizable = Optional.ofNullable(options.getAuthorizable())
                .orElse(context.determineAuthorizable(options.getAuthorizableId()));
        return authorizable.removeFromAllGroups();
    }

    public AclResult removeFromAllGroups(String authorizableId) {
        AuthorizableOptions options = new AuthorizableOptions();
        options.setAuthorizableId(authorizableId);
        return removeFromAllGroups(options);
    }

    public AclResult removeFromAllGroups(AclAuthorizable authorizable) {
        AuthorizableOptions options = new AuthorizableOptions();
        options.setAuthorizable(authorizable);
        return removeFromAllGroups(options);
    }

    public AclResult addMember(MemberOptions options) {
        AclGroup group = Optional.ofNullable(options.getGroup()).orElse(context.determineGroup(options.getGroupId()));
        return group.addMember(options);
    }

    public AclResult addMember(String groupId, String memberId) {
        MemberOptions options = new MemberOptions();
        options.setGroupId(groupId);
        options.setMemberId(memberId);
        return addMember(options);
    }

    public AclResult addMember(String groupId, AclAuthorizable member) {
        MemberOptions options = new MemberOptions();
        options.setGroupId(groupId);
        options.setMember(member);
        return addMember(options);
    }

    public AclResult addMember(AclGroup group, String memberId) {
        MemberOptions options = new MemberOptions();
        options.setGroup(group);
        options.setMemberId(memberId);
        return addMember(options);
    }

    public AclResult addMember(AclGroup group, AclAuthorizable member) {
        MemberOptions options = new MemberOptions();
        options.setGroup(group);
        options.setMember(member);
        return addMember(options);
    }

    public AclResult removeMember(MemberOptions options) {
        AclGroup group = Optional.ofNullable(options.getGroup()).orElse(context.determineGroup(options.getGroupId()));
        return group.removeMember(options);
    }

    public AclResult removeMember(String groupId, String memberId) {
        MemberOptions options = new MemberOptions();
        options.setGroupId(groupId);
        options.setMemberId(memberId);
        return removeMember(options);
    }

    public AclResult removeMember(String groupId, AclAuthorizable member) {
        MemberOptions options = new MemberOptions();
        options.setGroupId(groupId);
        options.setMember(member);
        return removeMember(options);
    }

    public AclResult removeMember(AclGroup group, String memberId) {
        MemberOptions options = new MemberOptions();
        options.setGroup(group);
        options.setMemberId(memberId);
        return removeMember(options);
    }

    public AclResult removeMember(AclGroup group, AclAuthorizable member) {
        MemberOptions options = new MemberOptions();
        options.setGroup(group);
        options.setMember(member);
        return removeMember(options);
    }

    public AclResult removeAllMembers(RemoveAllMembersOptions options) {
        AclGroup group = Optional.ofNullable(options.getGroup()).orElse(context.determineGroup(options.getGroupId()));
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
        AclAuthorizable authorizable = Optional.ofNullable(options.getAuthorizable())
                .orElse(context.determineAuthorizable(options.getAuthorizableId()));
        return authorizable.clear(options);
    }

    public AclResult clear(String authorizableId, String path, boolean strict) {
        ClearOptions options = new ClearOptions();
        options.setAuthorizableId(authorizableId);
        options.setPath(path);
        options.setStrict(strict);
        return clear(options);
    }

    public AclResult clear(String authorizableId, String path) {
        ClearOptions options = new ClearOptions();
        options.setAuthorizableId(authorizableId);
        options.setPath(path);
        return clear(options);
    }

    public AclResult clear(AclAuthorizable authorizable, String path, boolean strict) {
        ClearOptions options = new ClearOptions();
        options.setAuthorizable(authorizable);
        options.setPath(path);
        options.setStrict(strict);
        return clear(options);
    }

    public AclResult clear(AclAuthorizable authorizable, String path) {
        ClearOptions options = new ClearOptions();
        options.setAuthorizable(authorizable);
        options.setPath(path);
        return clear(options);
    }

    public AclResult purge(AuthorizableOptions options) {
        AclAuthorizable authorizable = Optional.ofNullable(options.getAuthorizable())
                .orElse(context.determineAuthorizable(options.getAuthorizableId()));
        return authorizable.purge();
    }

    public AclResult purge(String authorizableId) {
        AuthorizableOptions options = new AuthorizableOptions();
        options.setAuthorizableId(authorizableId);
        return purge(options);
    }

    public AclResult purge(AclAuthorizable authorizable) {
        AuthorizableOptions options = new AuthorizableOptions();
        options.setAuthorizable(authorizable);
        return purge(options);
    }

    public AclResult allow(PermissionsOptions options) {
        AclAuthorizable authorizable = Optional.ofNullable(options.getAuthorizable())
                .orElse(context.determineAuthorizable(options.getAuthorizableId()));
        return authorizable.allow(options);
    }

    public AclResult allow(
            String authorizableId,
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            PermissionsMode mode) {
        PermissionsOptions options = new PermissionsOptions();
        options.setAuthorizableId(authorizableId);
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
            AclAuthorizable authorizable,
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            PermissionsMode mode) {
        PermissionsOptions options = new PermissionsOptions();
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

    public AclResult allow(String authorizableId, String path, List<String> permissions) {
        PermissionsOptions options = new PermissionsOptions();
        options.setAuthorizableId(authorizableId);
        options.setPath(path);
        options.setPermissions(permissions);
        return allow(options);
    }

    public AclResult allow(AclAuthorizable authorizable, String path, List<String> permissions) {
        PermissionsOptions options = new PermissionsOptions();
        options.setAuthorizable(authorizable);
        options.setPath(path);
        options.setPermissions(permissions);
        return allow(options);
    }

    public AclResult allow(String authorizableId, String path, List<String> permissions, String glob) {
        PermissionsOptions options = new PermissionsOptions();
        options.setAuthorizableId(authorizableId);
        options.setPath(path);
        options.setPermissions(permissions);
        options.setGlob(glob);
        return allow(options);
    }

    public AclResult allow(AclAuthorizable authorizable, String path, List<String> permissions, String glob) {
        PermissionsOptions options = new PermissionsOptions();
        options.setAuthorizable(authorizable);
        options.setPath(path);
        options.setPermissions(permissions);
        options.setGlob(glob);
        return allow(options);
    }

    public AclResult allow(
            String authorizableId, String path, List<String> permissions, Map<String, Object> restrictions) {
        PermissionsOptions options = new PermissionsOptions();
        options.setAuthorizableId(authorizableId);
        options.setPath(path);
        options.setPermissions(permissions);
        options.setRestrictions(restrictions);
        return allow(options);
    }

    public AclResult allow(
            AclAuthorizable authorizable, String path, List<String> permissions, Map<String, Object> restrictions) {
        PermissionsOptions options = new PermissionsOptions();
        options.setAuthorizable(authorizable);
        options.setPath(path);
        options.setPermissions(permissions);
        options.setRestrictions(restrictions);
        return allow(options);
    }

    public AclResult deny(PermissionsOptions options) {
        AclAuthorizable authorizable = Optional.ofNullable(options.getAuthorizable())
                .orElse(context.determineAuthorizable(options.getAuthorizableId()));
        return authorizable.deny(options);
    }

    public AclResult deny(
            String authorizableId,
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            PermissionsMode mode) {
        PermissionsOptions options = new PermissionsOptions();
        options.setAuthorizableId(authorizableId);
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
            AclAuthorizable authorizable,
            String path,
            List<String> permissions,
            String glob,
            List<String> types,
            List<String> properties,
            Map<String, Object> restrictions,
            PermissionsMode mode) {
        PermissionsOptions options = new PermissionsOptions();
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

    public AclResult deny(String authorizableId, String path, List<String> permissions) {
        PermissionsOptions options = new PermissionsOptions();
        options.setAuthorizableId(authorizableId);
        options.setPath(path);
        options.setPermissions(permissions);
        return deny(options);
    }

    public AclResult deny(AclAuthorizable authorizable, String path, List<String> permissions) {
        PermissionsOptions options = new PermissionsOptions();
        options.setAuthorizable(authorizable);
        options.setPath(path);
        options.setPermissions(permissions);
        return deny(options);
    }

    public AclResult deny(String authorizableId, String path, List<String> permissions, String glob) {
        PermissionsOptions options = new PermissionsOptions();
        options.setAuthorizableId(authorizableId);
        options.setPath(path);
        options.setPermissions(permissions);
        options.setGlob(glob);
        return deny(options);
    }

    public AclResult deny(AclAuthorizable authorizable, String path, List<String> permissions, String glob) {
        PermissionsOptions options = new PermissionsOptions();
        options.setAuthorizable(authorizable);
        options.setPath(path);
        options.setPermissions(permissions);
        options.setGlob(glob);
        return deny(options);
    }

    public AclResult deny(
            String authorizableId, String path, List<String> permissions, Map<String, Object> restrictions) {
        PermissionsOptions options = new PermissionsOptions();
        options.setAuthorizableId(authorizableId);
        options.setPath(path);
        options.setPermissions(permissions);
        options.setRestrictions(restrictions);
        return deny(options);
    }

    public AclResult deny(
            AclAuthorizable authorizable, String path, List<String> permissions, Map<String, Object> restrictions) {
        PermissionsOptions options = new PermissionsOptions();
        options.setAuthorizable(authorizable);
        options.setPath(path);
        options.setPermissions(permissions);
        options.setRestrictions(restrictions);
        return deny(options);
    }

    public AclResult setProperty(SetPropertyOptions options) {
        AclAuthorizable authorizable = Optional.ofNullable(options.getAuthorizable())
                .orElse(context.determineAuthorizable(options.getAuthorizableId()));
        return authorizable.setProperty(options);
    }

    public AclResult setProperty(String authorizableId, String relPath, String value) {
        SetPropertyOptions options = new SetPropertyOptions();
        options.setAuthorizableId(authorizableId);
        options.setRelPath(relPath);
        options.setValue(value);
        return setProperty(options);
    }

    public AclResult setProperty(AclAuthorizable authorizable, String relPath, String value) {
        SetPropertyOptions options = new SetPropertyOptions();
        options.setAuthorizable(authorizable);
        options.setRelPath(relPath);
        options.setValue(value);
        return setProperty(options);
    }

    public AclResult removeProperty(RemovePropertyOptions options) {
        AclAuthorizable authorizable = Optional.ofNullable(options.getAuthorizable())
                .orElse(context.determineAuthorizable(options.getAuthorizableId()));
        return authorizable.removeProperty(options);
    }

    public AclResult removeProperty(String authorizableId, String relPath) {
        RemovePropertyOptions options = new RemovePropertyOptions();
        options.setAuthorizableId(authorizableId);
        options.setRelPath(relPath);
        return removeProperty(options);
    }

    public AclResult removeProperty(AclAuthorizable authorizable, String relPath) {
        RemovePropertyOptions options = new RemovePropertyOptions();
        options.setAuthorizable(authorizable);
        options.setRelPath(relPath);
        return removeProperty(options);
    }

    public AclResult setPassword(PasswordOptions options) {
        AclUser user = Optional.ofNullable(options.getUser()).orElse(context.determineUser(options.getUserId()));
        return user.setPassword(options);
    }

    public AclResult setPassword(String userId, String password) {
        PasswordOptions options = new PasswordOptions();
        options.setUserId(userId);
        options.setPassword(password);
        return setPassword(options);
    }

    public AclResult setPassword(AclUser user, String password) {
        PasswordOptions options = new PasswordOptions();
        options.setUser(user);
        options.setPassword(password);
        return setPassword(options);
    }

    public AclUser user(User user) {
        return context.determineUser(user);
    }

    public AclGroup group(Group group) {
        return context.determineGroup(group);
    }
}
