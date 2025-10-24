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

    public void deleteUser(Closure<DeleteUserOptions> closure) {
        deleteUser(GroovyUtils.with(new DeleteUserOptions(), closure));
    }

    public void deleteGroup(Closure<DeleteGroupOptions> closure) {
        deleteGroup(GroovyUtils.with(new DeleteGroupOptions(), closure));
    }

    public void addToGroup(Closure<GroupOptions> closure) {
        addToGroup(GroovyUtils.with(new GroupOptions(), closure));
    }

    public void removeFromGroup(Closure<GroupOptions> closure) {
        removeFromGroup(GroovyUtils.with(new GroupOptions(), closure));
    }

    public void removeFromAllGroups(Closure<AuthorizableOptions> closure) {
        removeFromAllGroups(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public void addMember(Closure<MemberOptions> closure) {
        addMember(GroovyUtils.with(new MemberOptions(), closure));
    }

    public void removeMember(Closure<MemberOptions> closure) {
        removeMember(GroovyUtils.with(new MemberOptions(), closure));
    }

    public void removeAllMembers(Closure<RemoveAllMembersOptions> closure) {
        removeAllMembers(GroovyUtils.with(new RemoveAllMembersOptions(), closure));
    }

    public void clear(Closure<ClearOptions> closure) {
        clear(GroovyUtils.with(new ClearOptions(), closure));
    }

    public void purge(Closure<AuthorizableOptions> closure) {
        purge(GroovyUtils.with(new AuthorizableOptions(), closure));
    }

    public void allow(Closure<PermissionsOptions> closure) {
        allow(GroovyUtils.with(new PermissionsOptions(), closure));
    }

    public void deny(Closure<PermissionsOptions> closure) {
        deny(GroovyUtils.with(new PermissionsOptions(), closure));
    }

    public void setProperty(Closure<SetPropertyOptions> closure) {
        setProperty(GroovyUtils.with(new SetPropertyOptions(), closure));
    }

    public void removeProperty(Closure<RemovePropertyOptions> closure) {
        removeProperty(GroovyUtils.with(new RemovePropertyOptions(), closure));
    }

    public void setPassword(Closure<PasswordOptions> closure) {
        setPassword(GroovyUtils.with(new PasswordOptions(), closure));
    }

    public void check(Closure<AclChecker> closure) {
        GroovyUtils.with(checker, closure);
    }

    public AclUser createUser(CreateUserOptions options) {
        User user = context.getAuthorizableManager().getUser(options.getId());
        boolean created = false;
        if (user == null) {
            if (options.isSystemUser()) {
                user = context.getAuthorizableManager().createSystemUser(options.getId(), options.getPath());
            } else {
                user = context.getAuthorizableManager()
                        .createUser(options.getId(), options.getPassword(), options.getPath());
            }
            context.getAuthorizableManager().updateUser(user, options.getPassword(), options.determineProperties());
            created = true;
        } else if (options.getMode() == CreateAuthorizableOptions.Mode.FAIL) {
            throw new AclException(String.format("User '%s' already exists", options.getId()));
        } else if (options.getMode() == CreateAuthorizableOptions.Mode.OVERRIDE) {
            context.getAuthorizableManager().updateUser(user, options.getPassword(), options.determineProperties());
            created = true;
        }
        AclUser aclUser = context.determineUser(user);
        if (created) {
            context.getLogger().info("Created user '{}'", aclUser.getId());
        } else {
            context.getLogger().info("User '{}' already exists", aclUser.getId());
        }
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
        boolean created = false;
        if (group == null) {
            group = context.getAuthorizableManager()
                    .createGroup(options.getId(), options.getPath(), options.getExternalId());
            context.getAuthorizableManager().updateGroup(group, options.determineProperties());
            created = true;
        } else if (options.getMode() == CreateAuthorizableOptions.Mode.FAIL) {
            throw new AclException(String.format("Group '%s' already exists", options.getId()));
        } else if (options.getMode() == CreateAuthorizableOptions.Mode.OVERRIDE) {
            context.getAuthorizableManager().updateGroup(group, options.determineProperties());
            created = true;
        }
        AclGroup aclGroup = context.determineGroup(group);
        if (created) {
            context.getLogger().info("Created group '{}'", aclGroup.getId());
        } else {
            context.getLogger().info("Group '{}' already exists", aclGroup.getId());
        }
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

    private void deleteAuthorizable(AclAuthorizable authorizable, String id) {
        if (authorizable == null) {
            context.getLogger().info("Authorizable '{}' not found (already deleted or never existed)", id);
            return;
        } 
        if (context.getAuthorizableManager().getAuthorizable(id) == null) {
            context.getLogger().info("Authorizable '{}' not found (already deleted)", id);
            return;
        }
        
        purge(authorizable);
        context.getAuthorizableManager().deleteAuthorizable(authorizable.get());
    }

    public void deleteUser(DeleteUserOptions options) {
        AclUser user = context.determineUser(options.getUser(), options.getId());
        String id = context.determineId(options.getUser(), options.getId());
        deleteAuthorizable(user, id);
        context.getLogger().info("Deleted user '{}'", id);
    }

    public void deleteUser(String id) {
        DeleteUserOptions options = new DeleteUserOptions();
        options.setId(id);
        deleteUser(options);
    }

    public void deleteUser(AclUser user) {
        DeleteUserOptions options = new DeleteUserOptions();
        options.setUser(user);
        deleteUser(options);
    }

    public void deleteGroup(DeleteGroupOptions options) {
        AclGroup group = context.determineGroup(options.getGroup(), options.getId());
        String id = context.determineId(options.getGroup(), options.getId());
        deleteAuthorizable(group, id);
        context.getLogger().info("Deleted group '{}'", id);
    }

    public void deleteGroup(String id) {
        DeleteGroupOptions options = new DeleteGroupOptions();
        options.setId(id);
        deleteGroup(options);
    }

    public void deleteGroup(AclGroup group) {
        DeleteGroupOptions options = new DeleteGroupOptions();
        options.setGroup(group);
        deleteGroup(options);
    }

    public void addToGroup(GroupOptions options) {
        AclAuthorizable authorizable =
                context.determineAuthorizable(options.getAuthorizable(), options.getAuthorizableId());
        if (authorizable == null) {
            String authorizableId = context.determineId(options.getAuthorizable(), options.getAuthorizableId());
            String groupId = context.determineId(options.getGroup(), options.getGroupId());
            context.getLogger()
                    .info("Skipped adding authorizable '{}' to group '{}' (authorizable not found)", authorizableId, groupId);
            return;
        }
        authorizable.addToGroup(options);
    }

    public void addToGroup(String authorizableId, String groupId) {
        GroupOptions options = new GroupOptions();
        options.setAuthorizableId(authorizableId);
        options.setGroupId(groupId);
        addToGroup(options);
    }

    public void addToGroup(AclAuthorizable authorizable, String groupId) {
        GroupOptions options = new GroupOptions();
        options.setAuthorizable(authorizable);
        options.setGroupId(groupId);
        addToGroup(options);
    }

    public void addToGroup(String authorizableId, AclGroup group) {
        GroupOptions options = new GroupOptions();
        options.setAuthorizableId(authorizableId);
        options.setGroup(group);
        addToGroup(options);
    }

    public void addToGroup(AclAuthorizable authorizable, AclGroup group) {
        GroupOptions options = new GroupOptions();
        options.setAuthorizable(authorizable);
        options.setGroup(group);
        addToGroup(options);
    }

    public void removeFromGroup(GroupOptions options) {
        AclAuthorizable authorizable =
                context.determineAuthorizable(options.getAuthorizable(), options.getAuthorizableId());
        if (authorizable == null) {
            String authorizableId = context.determineId(options.getAuthorizable(), options.getAuthorizableId());
            String groupId = context.determineId(options.getGroup(), options.getGroupId());
            context.getLogger()
                    .info("Skipped removing authorizable '{}' from group '{}' (authorizable not found)", authorizableId, groupId);
            return;
        }
        authorizable.removeFromGroup(options);
    }

    public void removeFromGroup(String authorizableId, String groupId) {
        GroupOptions options = new GroupOptions();
        options.setAuthorizableId(authorizableId);
        options.setGroupId(groupId);
        removeFromGroup(options);
    }

    public void removeFromGroup(AclAuthorizable authorizable, String groupId) {
        GroupOptions options = new GroupOptions();
        options.setAuthorizable(authorizable);
        options.setGroupId(groupId);
        removeFromGroup(options);
    }

    public void removeFromGroup(String authorizableId, AclGroup group) {
        GroupOptions options = new GroupOptions();
        options.setAuthorizableId(authorizableId);
        options.setGroup(group);
        removeFromGroup(options);
    }

    public void removeFromGroup(AclAuthorizable authorizable, AclGroup group) {
        GroupOptions options = new GroupOptions();
        options.setAuthorizable(authorizable);
        options.setGroup(group);
        removeFromGroup(options);
    }

    public void removeFromAllGroups(AuthorizableOptions options) {
        AclAuthorizable authorizable =
                context.determineAuthorizable(options.getAuthorizable(), options.getAuthorizableId());
        if (authorizable == null) {
            String authorizableId = context.determineId(options.getAuthorizable(), options.getAuthorizableId());
            context.getLogger()
                    .info("Skipped removing authorizable '{}' from all groups (authorizable not found)", authorizableId);
            return;
        }
        authorizable.removeFromAllGroups();
    }

    public void removeFromAllGroups(String authorizableId) {
        AuthorizableOptions options = new AuthorizableOptions();
        options.setAuthorizableId(authorizableId);
        removeFromAllGroups(options);
    }

    public void removeFromAllGroups(AclAuthorizable authorizable) {
        AuthorizableOptions options = new AuthorizableOptions();
        options.setAuthorizable(authorizable);
        removeFromAllGroups(options);
    }

    public void addMember(MemberOptions options) {
        AclGroup group = context.determineGroup(options.getGroup(), options.getGroupId());
        if (group == null) {
            String memberId = context.determineId(options.getMember(), options.getMemberId());
            String groupId = context.determineId(options.getGroup(), options.getGroupId());
            context.getLogger().info("Skipped adding member '{}' to group '{}' (group not found)", memberId, groupId);
            return;
        }
        group.addMember(options);
    }

    public void addMember(String groupId, String memberId) {
        MemberOptions options = new MemberOptions();
        options.setGroupId(groupId);
        options.setMemberId(memberId);
        addMember(options);
    }

    public void addMember(AclGroup group, String memberId) {
        MemberOptions options = new MemberOptions();
        options.setGroup(group);
        options.setMemberId(memberId);
        addMember(options);
    }

    public void addMember(String groupId, AclAuthorizable member) {
        MemberOptions options = new MemberOptions();
        options.setGroupId(groupId);
        options.setMember(member);
        addMember(options);
    }

    public void addMember(AclGroup group, AclAuthorizable member) {
        MemberOptions options = new MemberOptions();
        options.setGroup(group);
        options.setMember(member);
        addMember(options);
    }

    public void removeMember(MemberOptions options) {
        AclGroup group = context.determineGroup(options.getGroup(), options.getGroupId());
        if (group == null) {
            String memberId = context.determineId(options.getMember(), options.getMemberId());
            String groupId = context.determineId(options.getGroup(), options.getGroupId());
            context.getLogger().info("Skipped removing member '{}' from group '{}' (group not found)", memberId, groupId);
            return;
        }
        group.removeMember(options);
    }

    public void removeMember(String groupId, String memberId) {
        MemberOptions options = new MemberOptions();
        options.setGroupId(groupId);
        options.setMemberId(memberId);
        removeMember(options);
    }

    public void removeMember(AclGroup group, String memberId) {
        MemberOptions options = new MemberOptions();
        options.setGroup(group);
        options.setMemberId(memberId);
        removeMember(options);
    }

    public void removeMember(String groupId, AclAuthorizable member) {
        MemberOptions options = new MemberOptions();
        options.setGroupId(groupId);
        options.setMember(member);
        removeMember(options);
    }

    public void removeMember(AclGroup group, AclAuthorizable member) {
        MemberOptions options = new MemberOptions();
        options.setGroup(group);
        options.setMember(member);
        removeMember(options);
    }

    public void removeAllMembers(RemoveAllMembersOptions options) {
        AclGroup group = context.determineGroup(options.getGroup(), options.getGroupId());
        if (group == null) {
            String groupId = context.determineId(options.getGroup(), options.getGroupId());
            context.getLogger().info("Skipped removing all members from group '{}' (group not found)", groupId);
            return;
        }
        group.removeAllMembers();
    }

    public void removeAllMembers(String groupId) {
        RemoveAllMembersOptions options = new RemoveAllMembersOptions();
        options.setGroupId(groupId);
        removeAllMembers(options);
    }

    public void removeAllMembers(AclGroup group) {
        RemoveAllMembersOptions options = new RemoveAllMembersOptions();
        options.setGroup(group);
        removeAllMembers(options);
    }

    public void clear(ClearOptions options) {
        AclAuthorizable authorizable =
                context.determineAuthorizable(options.getAuthorizable(), options.getAuthorizableId());
        if (authorizable == null) {
            String authorizableId = context.determineId(options.getAuthorizable(), options.getAuthorizableId());
            context.getLogger()
                    .info(
                            "Skipped clearing permissions for authorizable '{}' at path '{}' (authorizable not found)",
                            authorizableId,
                            options.getPath());
            return;
        }
        authorizable.clear(options);
    }

    public void clear(String authorizableId, String path, boolean strict) {
        ClearOptions options = new ClearOptions();
        options.setAuthorizableId(authorizableId);
        options.setPath(path);
        options.setStrict(strict);
        clear(options);
    }

    public void clear(AclAuthorizable authorizable, String path, boolean strict) {
        ClearOptions options = new ClearOptions();
        options.setAuthorizable(authorizable);
        options.setPath(path);
        options.setStrict(strict);
        clear(options);
    }

    public void clear(String authorizableId, String path) {
        ClearOptions options = new ClearOptions();
        options.setAuthorizableId(authorizableId);
        options.setPath(path);
        clear(options);
    }

    public void clear(AclAuthorizable authorizable, String path) {
        ClearOptions options = new ClearOptions();
        options.setAuthorizable(authorizable);
        options.setPath(path);
        clear(options);
    }

    public void purge(AuthorizableOptions options) {
        AclAuthorizable authorizable =
                context.determineAuthorizable(options.getAuthorizable(), options.getAuthorizableId());
        if (authorizable == null) {
            String authorizableId = context.determineId(options.getAuthorizable(), options.getAuthorizableId());
            context.getLogger().info("Skipped purging authorizable '{}' (authorizable not found)", authorizableId);
            return;
        }
        authorizable.purge();
    }

    public void purge(String authorizableId) {
        AuthorizableOptions options = new AuthorizableOptions();
        options.setAuthorizableId(authorizableId);
        purge(options);
    }

    public void purge(AclAuthorizable authorizable) {
        AuthorizableOptions options = new AuthorizableOptions();
        options.setAuthorizable(authorizable);
        purge(options);
    }

    private void apply(PermissionsOptions options, boolean allow) {
        AclAuthorizable authorizable =
                context.determineAuthorizable(options.getAuthorizable(), options.getAuthorizableId());
        if (authorizable == null) {
            String authorizableId = context.determineId(options.getAuthorizable(), options.getAuthorizableId());
            String actionDescription = allow ? "allow permissions" : "deny permissions";
            context.getLogger()
                    .info(
                            "Skipped setting {} for authorizable '{}' at path '{}' (authorizable not found)",
                            actionDescription,
                            authorizableId,
                            options.getPath());
            return;
        }
        if (allow) {
            authorizable.allow(options);
        } else {
            authorizable.deny(options);
        }
    }

    public void allow(PermissionsOptions options) {
        apply(options, true);
    }

    public void deny(PermissionsOptions options) {
        apply(options, false);
    }

    public void setProperty(SetPropertyOptions options) {
        AclAuthorizable authorizable =
                context.determineAuthorizable(options.getAuthorizable(), options.getAuthorizableId());
        if (authorizable == null) {
            String authorizableId = context.determineId(options.getAuthorizable(), options.getAuthorizableId());
            context.getLogger()
                    .info(
                            "Skipped setting property '{}' for authorizable '{}' (authorizable not found)",
                            options.getRelPath(),
                            authorizableId);
            return;
        }
        authorizable.setProperty(options);
    }

    public void setProperty(String authorizableId, String relPath, String value) {
        SetPropertyOptions options = new SetPropertyOptions();
        options.setAuthorizableId(authorizableId);
        options.setRelPath(relPath);
        options.setValue(value);
        setProperty(options);
    }

    public void setProperty(AclAuthorizable authorizable, String relPath, String value) {
        SetPropertyOptions options = new SetPropertyOptions();
        options.setAuthorizable(authorizable);
        options.setRelPath(relPath);
        options.setValue(value);
        setProperty(options);
    }

    public void removeProperty(RemovePropertyOptions options) {
        AclAuthorizable authorizable =
                context.determineAuthorizable(options.getAuthorizable(), options.getAuthorizableId());
        if (authorizable == null) {
            String authorizableId = context.determineId(options.getAuthorizable(), options.getAuthorizableId());
            context.getLogger()
                    .info(
                            "Skipped removing property '{}' for authorizable '{}' (authorizable not found)",
                            options.getRelPath(),
                            authorizableId);
            return;
        }
        authorizable.removeProperty(options);
    }

    public void removeProperty(String authorizableId, String relPath) {
        RemovePropertyOptions options = new RemovePropertyOptions();
        options.setAuthorizableId(authorizableId);
        options.setRelPath(relPath);
        removeProperty(options);
    }

    public void removeProperty(AclAuthorizable authorizable, String relPath) {
        RemovePropertyOptions options = new RemovePropertyOptions();
        options.setAuthorizable(authorizable);
        options.setRelPath(relPath);
        removeProperty(options);
    }

    public void setPassword(PasswordOptions options) {
        AclUser user = context.determineUser(options.getUser(), options.getUserId());
        if (user == null) {
            String userId = context.determineId(options.getUser(), options.getUserId());
            context.getLogger().info("Skipped setting password for user '{}' (user not found)", userId);
            return;
        }
        user.setPassword(options);
    }

    public void setPassword(String userId, String password) {
        PasswordOptions options = new PasswordOptions();
        options.setUserId(userId);
        options.setPassword(password);
        setPassword(options);
    }

    public void setPassword(AclUser user, String password) {
        PasswordOptions options = new PasswordOptions();
        options.setUser(user);
        options.setPassword(password);
        setPassword(options);
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
