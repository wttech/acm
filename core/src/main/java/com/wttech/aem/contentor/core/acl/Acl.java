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
            boolean systemUser,
            String password,
            String path,
            String givenName,
            String familyName,
            String email,
            String aboutMe,
            CreateAuthorizableOptions.Mode mode) {
        return createUser(
                new CreateUserOptions(id, systemUser, password, path, givenName, familyName, email, aboutMe, mode));
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
        context.getLogger().info("Created user '{}' [{}]", aclUser.getId(), result);
        return aclUser;
    }

    public AclUser createUser(String id) {
        return createUser(new CreateUserOptions(id, false, null, null, null, null, null, null, null));
    }

    public AclUser createUser(String id, String password) {
        return createUser(new CreateUserOptions(id, false, password, null, null, null, null, null, null));
    }

    public AclUser createSystemUser(String id) {
        return createUser(new CreateUserOptions(id, true, null, null, null, null, null, null, null));
    }

    public AclGroup createGroup(
            String id,
            String externalId,
            String path,
            String givenName,
            String email,
            String aboutMe,
            CreateAuthorizableOptions.Mode mode) {
        return createGroup(new CreateGroupOptions(id, externalId, path, givenName, email, aboutMe, mode));
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
        context.getLogger().info("Created group '{}' [{}]", aclGroup.getId(), result);
        return aclGroup;
    }

    public AclGroup createGroup(String id) {
        return createGroup(new CreateGroupOptions(id, null, null, null, null, null, null));
    }

    public AclGroup createGroup(String id, String externalId) {
        return createGroup(new CreateGroupOptions(id, externalId, null, null, null, null, null));
    }

    public AclUser getUser(GetAuthorizableOptions options) {
        return getUser(options.getId());
    }

    public AclUser getUser(String id) {
        return context.determineUser(id);
    }

    public AclGroup getGroup(GetAuthorizableOptions options) {
        return getGroup(options.getId());
    }

    public AclGroup getGroup(String id) {
        return context.determineGroup(id);
    }

    private AclResult deleteAuthorizable(AclAuthorizable authorizable, String id) {
        AclResult result;
        if (authorizable == null) {
            result = AclResult.OK;
        } else if (context.getAuthorizableManager().getAuthorizable(id) == null) {
            result = AclResult.OK;
        } else {
            purge(authorizable);
            context.getAuthorizableManager().deleteAuthorizable(authorizable.get());
            result = AclResult.CHANGED;
        }
        return result;
    }

    public AclResult deleteUser(DeleteUserOptions options) {
        AclUser user = context.determineUser(options.getUser(), options.getId());
        String id = context.determineId(options.getUser(), options.getId());
        AclResult result = deleteAuthorizable(user, id);
        context.getLogger().info("Deleted user '{}' [{}]", id, result);
        return result;
    }

    public AclResult deleteUser(String id) {
        return deleteUser(new DeleteUserOptions(null, id));
    }

    public AclResult deleteUser(AclUser user) {
        return deleteUser(new DeleteUserOptions(user, null));
    }

    public AclResult deleteGroup(DeleteGroupOptions options) {
        AclGroup group = context.determineGroup(options.getGroup(), options.getId());
        String id = context.determineId(options.getGroup(), options.getId());
        AclResult result = deleteAuthorizable(group, id);
        context.getLogger().info("Deleted group '{}' [{}]", id, result);
        return result;
    }

    public AclResult deleteGroup(String id) {
        return deleteGroup(new DeleteGroupOptions(null, id));
    }

    public AclResult deleteGroup(AclGroup group) {
        return deleteGroup(new DeleteGroupOptions(group, null));
    }

    public AclResult addToGroup(GroupOptions options) {
        AclAuthorizable authorizable =
                context.determineAuthorizable(options.getAuthorizable(), options.getAuthorizableId());
        if (authorizable == null) {
            String authorizableId = context.determineId(options.getAuthorizable(), options.getAuthorizableId());
            String groupId = context.determineId(options.getGroup(), options.getGroupId());
            context.getLogger()
                    .info("Added authorizable '{}' to group '{}' [{}]", authorizableId, groupId, AclResult.SKIPPED);
            return AclResult.SKIPPED;
        }
        return authorizable.addToGroup(options);
    }

    public AclResult addToGroup(String authorizableId, String groupId) {
        return addToGroup(new GroupOptions(null, authorizableId, null, groupId));
    }

    public AclResult addToGroup(String authorizableId, AclGroup group) {
        return addToGroup(new GroupOptions(null, authorizableId, group, null));
    }

    public AclResult addToGroup(AclAuthorizable authorizable, String groupId) {
        return addToGroup(new GroupOptions(authorizable, null, null, groupId));
    }

    public AclResult addToGroup(AclAuthorizable authorizable, AclGroup group) {
        return addToGroup(new GroupOptions(authorizable, null, group, null));
    }

    public AclResult removeFromGroup(GroupOptions options) {
        AclAuthorizable authorizable =
                context.determineAuthorizable(options.getAuthorizable(), options.getAuthorizableId());
        if (authorizable == null) {
            String authorizableId = context.determineId(options.getAuthorizable(), options.getAuthorizableId());
            String groupId = context.determineId(options.getGroup(), options.getGroupId());
            context.getLogger()
                    .info("Removed authorizable '{}' from group '{}' [{}]", authorizableId, groupId, AclResult.SKIPPED);
            return AclResult.SKIPPED;
        }
        return authorizable.removeFromGroup(options);
    }

    public AclResult removeFromGroup(String authorizableId, String groupId) {
        return removeFromGroup(new GroupOptions(null, authorizableId, null, groupId));
    }

    public AclResult removeFromGroup(String authorizableId, AclGroup group) {
        return removeFromGroup(new GroupOptions(null, authorizableId, group, null));
    }

    public AclResult removeFromGroup(AclAuthorizable authorizable, String groupId) {
        return removeFromGroup(new GroupOptions(authorizable, null, null, groupId));
    }

    public AclResult removeFromGroup(AclAuthorizable authorizable, AclGroup group) {
        return removeFromGroup(new GroupOptions(authorizable, null, group, null));
    }

    public AclResult removeFromAllGroups(AuthorizableOptions options) {
        AclAuthorizable authorizable =
                context.determineAuthorizable(options.getAuthorizable(), options.getAuthorizableId());
        if (authorizable == null) {
            String authorizableId = context.determineId(options.getAuthorizable(), options.getAuthorizableId());
            context.getLogger()
                    .info("Removed authorizable '{}' from all groups [{}]", authorizableId, AclResult.SKIPPED);
            return AclResult.SKIPPED;
        }
        return authorizable.removeFromAllGroups();
    }

    public AclResult removeFromAllGroups(String authorizableId) {
        return removeFromAllGroups(new AuthorizableOptions(null, authorizableId));
    }

    public AclResult removeFromAllGroups(AclAuthorizable authorizable) {
        return removeFromAllGroups(new AuthorizableOptions(authorizable, null));
    }

    public AclResult addMember(MemberOptions options) {
        AclGroup group = context.determineGroup(options.getGroup(), options.getGroupId());
        if (group == null) {
            String memberId = context.determineId(options.getMember(), options.getMemberId());
            String groupId = context.determineId(options.getGroup(), options.getGroupId());
            context.getLogger().info("Added member '{}' to group '{}' [{}]", memberId, groupId, AclResult.SKIPPED);
            return AclResult.SKIPPED;
        }
        return group.addMember(options);
    }

    public AclResult addMember(String groupId, String memberId) {
        return addMember(new MemberOptions(null, groupId, null, memberId));
    }

    public AclResult addMember(String groupId, AclAuthorizable member) {
        return addMember(new MemberOptions(null, groupId, member, null));
    }

    public AclResult addMember(AclGroup group, String memberId) {
        return addMember(new MemberOptions(group, null, null, memberId));
    }

    public AclResult addMember(AclGroup group, AclAuthorizable member) {
        return addMember(new MemberOptions(group, null, member, null));
    }

    public AclResult removeMember(MemberOptions options) {
        AclGroup group = context.determineGroup(options.getGroup(), options.getGroupId());
        if (group == null) {
            String memberId = context.determineId(options.getMember(), options.getMemberId());
            String groupId = context.determineId(options.getGroup(), options.getGroupId());
            context.getLogger().info("Removed member '{}' from group '{}' [{}]", memberId, groupId, AclResult.SKIPPED);
            return AclResult.SKIPPED;
        }
        return group.removeMember(options);
    }

    public AclResult removeMember(String groupId, String memberId) {
        return removeMember(new MemberOptions(null, groupId, null, memberId));
    }

    public AclResult removeMember(String groupId, AclAuthorizable member) {
        return removeMember(new MemberOptions(null, groupId, member, null));
    }

    public AclResult removeMember(AclGroup group, String memberId) {
        return removeMember(new MemberOptions(group, null, null, memberId));
    }

    public AclResult removeMember(AclGroup group, AclAuthorizable member) {
        return removeMember(new MemberOptions(group, null, member, null));
    }

    public AclResult removeAllMembers(RemoveAllMembersOptions options) {
        AclGroup group = context.determineGroup(options.getGroup(), options.getGroupId());
        if (group == null) {
            String groupId = context.determineId(options.getGroup(), options.getGroupId());
            context.getLogger().info("Removed all members from group '{}' [{}]", groupId, AclResult.SKIPPED);
            return AclResult.SKIPPED;
        }
        return group.removeAllMembers();
    }

    public AclResult removeAllMembers(String groupId) {
        return removeAllMembers(new RemoveAllMembersOptions(null, groupId));
    }

    public AclResult removeAllMembers(AclGroup group) {
        return removeAllMembers(new RemoveAllMembersOptions(group, null));
    }

    public AclResult clear(ClearOptions options) {
        AclAuthorizable authorizable =
                context.determineAuthorizable(options.getAuthorizable(), options.getAuthorizableId());
        if (authorizable == null) {
            String authorizableId = context.determineId(options.getAuthorizable(), options.getAuthorizableId());
            context.getLogger()
                    .info(
                            "Cleared permissions for authorizable '{}' at path '{}' [{}]",
                            authorizableId,
                            options.getPath(),
                            AclResult.SKIPPED);
            return AclResult.SKIPPED;
        }
        return authorizable.clear(options);
    }

    public AclResult clear(String authorizableId, String path, boolean strict) {
        return clear(new ClearOptions(null, authorizableId, path, strict));
    }

    public AclResult clear(String authorizableId, String path) {
        return clear(new ClearOptions(null, authorizableId, path, false));
    }

    public AclResult clear(AclAuthorizable authorizable, String path, boolean strict) {
        return clear(new ClearOptions(authorizable, null, path, strict));
    }

    public AclResult clear(AclAuthorizable authorizable, String path) {
        return clear(new ClearOptions(authorizable, null, path, false));
    }

    public AclResult purge(AuthorizableOptions options) {
        AclAuthorizable authorizable =
                context.determineAuthorizable(options.getAuthorizable(), options.getAuthorizableId());
        if (authorizable == null) {
            String authorizableId = context.determineId(options.getAuthorizable(), options.getAuthorizableId());
            context.getLogger().info("Purged authorizable '{}' [{}]", authorizableId, AclResult.SKIPPED);
            return AclResult.SKIPPED;
        }
        return authorizable.purge();
    }

    public AclResult purge(String authorizableId) {
        return purge(new AuthorizableOptions(null, authorizableId));
    }

    public AclResult purge(AclAuthorizable authorizable) {
        return purge(new AuthorizableOptions(authorizable, null));
    }

    public AclResult allow(PermissionsOptions options) {
        AclAuthorizable authorizable =
                context.determineAuthorizable(options.getAuthorizable(), options.getAuthorizableId());
        if (authorizable == null) {
            String authorizableId = context.determineId(options.getAuthorizable(), options.getAuthorizableId());
            context.getLogger()
                    .info(
                            "Applied allow permissions for authorizable '{}' at path '{}' [{}]",
                            authorizableId,
                            options.getPath(),
                            AclResult.SKIPPED);
            return AclResult.SKIPPED;
        }
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
            PermissionsOptions.Mode mode) {
        return allow(new PermissionsOptions(
                null, authorizableId, path, permissions, glob, types, properties, restrictions, mode));
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
        return allow(new PermissionsOptions(
                authorizable, null, path, permissions, glob, types, properties, restrictions, mode));
    }

    public AclResult allow(String authorizableId, String path, List<String> permissions) {
        return allow(authorizableId, path, permissions, null, null, null, null, null);
    }

    public AclResult allow(AclAuthorizable authorizable, String path, List<String> permissions) {
        return allow(authorizable, path, permissions, null, null, null, null, null);
    }

    public AclResult allow(String authorizableId, String path, List<String> permissions, String glob) {
        return allow(authorizableId, path, permissions, glob, null, null, null, null);
    }

    public AclResult allow(AclAuthorizable authorizable, String path, List<String> permissions, String glob) {
        return allow(authorizable, path, permissions, glob, null, null, null, null);
    }

    public AclResult allow(
            String authorizableId, String path, List<String> permissions, Map<String, Object> restrictions) {
        return allow(authorizableId, path, permissions, null, null, null, restrictions, null);
    }

    public AclResult allow(
            AclAuthorizable authorizable, String path, List<String> permissions, Map<String, Object> restrictions) {
        return allow(authorizable, path, permissions, null, null, null, restrictions, null);
    }

    public AclResult deny(PermissionsOptions options) {
        AclAuthorizable authorizable =
                context.determineAuthorizable(options.getAuthorizable(), options.getAuthorizableId());
        if (authorizable == null) {
            String authorizableId = context.determineId(options.getAuthorizable(), options.getAuthorizableId());
            context.getLogger()
                    .info(
                            "Applied deny permissions for authorizable '{}' at path '{}' [{}]",
                            authorizableId,
                            options.getPath(),
                            AclResult.SKIPPED);
            return AclResult.SKIPPED;
        }
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
            PermissionsOptions.Mode mode) {
        return deny(new PermissionsOptions(
                null, authorizableId, path, permissions, glob, types, properties, restrictions, mode));
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
        return deny(new PermissionsOptions(
                authorizable, null, path, permissions, glob, types, properties, restrictions, mode));
    }

    public AclResult deny(String authorizableId, String path, List<String> permissions) {
        return deny(authorizableId, path, permissions, null, null, null, null, null);
    }

    public AclResult deny(AclAuthorizable authorizable, String path, List<String> permissions) {
        return deny(authorizable, path, permissions, null, null, null, null, null);
    }

    public AclResult deny(String authorizableId, String path, List<String> permissions, String glob) {
        return deny(authorizableId, path, permissions, glob, null, null, null, null);
    }

    public AclResult deny(AclAuthorizable authorizable, String path, List<String> permissions, String glob) {
        return deny(authorizable, path, permissions, glob, null, null, null, null);
    }

    public AclResult deny(
            String authorizableId, String path, List<String> permissions, Map<String, Object> restrictions) {
        return deny(authorizableId, path, permissions, null, null, null, restrictions, null);
    }

    public AclResult deny(
            AclAuthorizable authorizable, String path, List<String> permissions, Map<String, Object> restrictions) {
        return deny(authorizable, path, permissions, null, null, null, restrictions, null);
    }

    public AclResult setProperty(SetPropertyOptions options) {
        AclAuthorizable authorizable =
                context.determineAuthorizable(options.getAuthorizable(), options.getAuthorizableId());
        if (authorizable == null) {
            String authorizableId = context.determineId(options.getAuthorizable(), options.getAuthorizableId());
            context.getLogger()
                    .info(
                            "Set property '{}' for authorizable '{}' [{}]",
                            options.getRelPath(),
                            authorizableId,
                            AclResult.SKIPPED);
            return AclResult.SKIPPED;
        }
        return authorizable.setProperty(options);
    }

    public AclResult setProperty(String authorizableId, String relPath, String value) {
        return setProperty(new SetPropertyOptions(null, authorizableId, relPath, value));
    }

    public AclResult setProperty(AclAuthorizable authorizable, String relPath, String value) {
        return setProperty(new SetPropertyOptions(authorizable, null, relPath, value));
    }

    public AclResult removeProperty(RemovePropertyOptions options) {
        AclAuthorizable authorizable =
                context.determineAuthorizable(options.getAuthorizable(), options.getAuthorizableId());
        if (authorizable == null) {
            String authorizableId = context.determineId(options.getAuthorizable(), options.getAuthorizableId());
            context.getLogger()
                    .info(
                            "Removed property '{}' for authorizable '{}' [{}]",
                            options.getRelPath(),
                            authorizableId,
                            AclResult.SKIPPED);
            return AclResult.SKIPPED;
        }
        return authorizable.removeProperty(options);
    }

    public AclResult removeProperty(String authorizableId, String relPath) {
        return removeProperty(new RemovePropertyOptions(null, authorizableId, relPath));
    }

    public AclResult removeProperty(AclAuthorizable authorizable, String relPath) {
        return removeProperty(new RemovePropertyOptions(authorizable, null, relPath));
    }

    public AclResult setPassword(PasswordOptions options) {
        AclUser user = context.determineUser(options.getUser(), options.getUserId());
        if (user == null) {
            String userId = context.determineId(options.getUser(), options.getUserId());
            context.getLogger().info("Set password for user '{}' [{}]", userId, AclResult.SKIPPED);
            return AclResult.SKIPPED;
        }
        return user.setPassword(options);
    }

    public AclResult setPassword(String userId, String password) {
        return setPassword(new PasswordOptions(null, userId, password));
    }

    public AclResult setPassword(AclUser user, String password) {
        return setPassword(new PasswordOptions(user, null, password));
    }

    public AclUser user(User user) {
        return context.determineUser(user);
    }

    public AclGroup group(Group group) {
        return context.determineGroup(group);
    }
}
