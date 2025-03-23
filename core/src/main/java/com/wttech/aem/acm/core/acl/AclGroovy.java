package com.wttech.aem.acm.core.acl;

import com.wttech.aem.acm.core.acl.authorizable.AclGroup;
import com.wttech.aem.acm.core.acl.authorizable.AclUser;
import com.wttech.aem.acm.core.util.GroovyUtils;
import groovy.lang.Closure;
import org.apache.sling.api.resource.ResourceResolver;

public class AclGroovy extends Acl {

    public AclGroovy(ResourceResolver resourceResolver) {
        super(resourceResolver);
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
}
