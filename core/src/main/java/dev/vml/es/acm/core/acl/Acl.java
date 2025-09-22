package dev.vml.es.acm.core.acl;

import dev.vml.es.acm.core.acl.authorizable.AclAuthorizable;
import dev.vml.es.acm.core.acl.authorizable.AclGroup;
import dev.vml.es.acm.core.acl.authorizable.AclUser;
import dev.vml.es.acm.core.util.GroovyUtils;
import groovy.lang.Closure;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.resource.ResourceResolver;

public class Acl {

    private final AclContext context;

    private final AclChecker checker;

    public Acl(ResourceResolver resourceResolver) {
        this.context = new AclContext(resourceResolver);
        this.checker = new AclChecker(context);
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

    public void check(Closure<AclChecker> closure) {
        GroovyUtils.with(checker, closure);
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
        CreateUserOptions options = new CreateUserOptions();
        options.setId(id);
        return createUser(options);
    }

    public AclUser createUser(String id, String password) {
        CreateUserOptions options = new CreateUserOptions();
        options.setId(id);
        options.setPassword(password);
        return createUser(options);
    }

    public AclUser createSystemUser(String id) {
        CreateUserOptions options = new CreateUserOptions();
        options.setId(id);
        options.systemUser();
        return createUser(options);
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
        AclGroup group = context.determineGroup(options.getGroup(), options.getId());
        String id = context.determineId(options.getGroup(), options.getId());
        AclResult result = deleteAuthorizable(group, id);
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
        GroupOptions options = new GroupOptions();
        options.setAuthorizableId(authorizableId);
        options.setGroupId(groupId);
        return addToGroup(options);
    }

    public AclResult addToGroup(AclAuthorizable authorizable, String groupId) {
        GroupOptions options = new GroupOptions();
        options.setAuthorizable(authorizable);
        options.setGroupId(groupId);
        return addToGroup(options);
    }

    public AclResult addToGroup(String authorizableId, AclGroup group) {
        GroupOptions options = new GroupOptions();
        options.setAuthorizableId(authorizableId);
        options.setGroup(group);
        return addToGroup(options);
    }

    public AclResult addToGroup(AclAuthorizable authorizable, AclGroup group) {
        GroupOptions options = new GroupOptions();
        options.setAuthorizable(authorizable);
        options.setGroup(group);
        return addToGroup(options);
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
        GroupOptions options = new GroupOptions();
        options.setAuthorizableId(authorizableId);
        options.setGroupId(groupId);
        return removeFromGroup(options);
    }

    public AclResult removeFromGroup(AclAuthorizable authorizable, String groupId) {
        GroupOptions options = new GroupOptions();
        options.setAuthorizable(authorizable);
        options.setGroupId(groupId);
        return removeFromGroup(options);
    }

    public AclResult removeFromGroup(String authorizableId, AclGroup group) {
        GroupOptions options = new GroupOptions();
        options.setAuthorizableId(authorizableId);
        options.setGroup(group);
        return removeFromGroup(options);
    }

    public AclResult removeFromGroup(AclAuthorizable authorizable, AclGroup group) {
        GroupOptions options = new GroupOptions();
        options.setAuthorizable(authorizable);
        options.setGroup(group);
        return removeFromGroup(options);
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
        MemberOptions options = new MemberOptions();
        options.setGroupId(groupId);
        options.setMemberId(memberId);
        return addMember(options);
    }

    public AclResult addMember(AclGroup group, String memberId) {
        MemberOptions options = new MemberOptions();
        options.setGroup(group);
        options.setMemberId(memberId);
        return addMember(options);
    }

    public AclResult addMember(String groupId, AclAuthorizable member) {
        MemberOptions options = new MemberOptions();
        options.setGroupId(groupId);
        options.setMember(member);
        return addMember(options);
    }

    public AclResult addMember(AclGroup group, AclAuthorizable member) {
        MemberOptions options = new MemberOptions();
        options.setGroup(group);
        options.setMember(member);
        return addMember(options);
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
        MemberOptions options = new MemberOptions();
        options.setGroupId(groupId);
        options.setMemberId(memberId);
        return removeMember(options);
    }

    public AclResult removeMember(AclGroup group, String memberId) {
        MemberOptions options = new MemberOptions();
        options.setGroup(group);
        options.setMemberId(memberId);
        return removeMember(options);
    }

    public AclResult removeMember(String groupId, AclAuthorizable member) {
        MemberOptions options = new MemberOptions();
        options.setGroupId(groupId);
        options.setMember(member);
        return removeMember(options);
    }

    public AclResult removeMember(AclGroup group, AclAuthorizable member) {
        MemberOptions options = new MemberOptions();
        options.setGroup(group);
        options.setMember(member);
        return removeMember(options);
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
        RemoveAllMembersOptions options = new RemoveAllMembersOptions();
        options.setGroupId(groupId);
        return removeAllMembers(options);
    }

    public AclResult removeAllMembers(AclGroup group) {
        RemoveAllMembersOptions options = new RemoveAllMembersOptions();
        options.setGroup(group);
        return removeAllMembers(options);
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
        ClearOptions options = new ClearOptions();
        options.setAuthorizableId(authorizableId);
        options.setPath(path);
        options.setStrict(strict);
        return clear(options);
    }

    public AclResult clear(AclAuthorizable authorizable, String path, boolean strict) {
        ClearOptions options = new ClearOptions();
        options.setAuthorizable(authorizable);
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

    public AclResult clear(AclAuthorizable authorizable, String path) {
        ClearOptions options = new ClearOptions();
        options.setAuthorizable(authorizable);
        options.setPath(path);
        return clear(options);
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
        AuthorizableOptions options = new AuthorizableOptions();
        options.setAuthorizableId(authorizableId);
        return purge(options);
    }

    public AclResult purge(AclAuthorizable authorizable) {
        AuthorizableOptions options = new AuthorizableOptions();
        options.setAuthorizable(authorizable);
        return purge(options);
    }

    private AclResult apply(PermissionsOptions options, boolean allow) {
        AclAuthorizable authorizable =
                context.determineAuthorizable(options.getAuthorizable(), options.getAuthorizableId());
        if (authorizable == null) {
            String authorizableId = context.determineId(options.getAuthorizable(), options.getAuthorizableId());
            context.getLogger()
                    .info(
                            "Applied {} permissions for authorizable '{}' at path '{}' [{}]",
                            allow ? "allow" : "deny",
                            authorizableId,
                            options.getPath(),
                            AclResult.SKIPPED);
            return AclResult.SKIPPED;
        }
        return allow ? authorizable.allow(options) : authorizable.deny(options);
    }

    public AclResult allow(PermissionsOptions options) {
        return apply(options, true);
    }

    public AclResult deny(PermissionsOptions options) {
        return apply(options, false);
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
        AclUser user = context.determineUser(options.getUser(), options.getUserId());
        if (user == null) {
            String userId = context.determineId(options.getUser(), options.getUserId());
            context.getLogger().info("Set password for user '{}' [{}]", userId, AclResult.SKIPPED);
            return AclResult.SKIPPED;
        }
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

    public AclAuthorizable authorizable(Authorizable authorizable) {
        return context.determineAuthorizable(authorizable);
    }

    public AclChecker getChecker() {
        return checker;
    }
}
