package com.wttech.aem.contentor.core.acl.authorizable;

import com.wttech.aem.contentor.core.acl.AclException;
import com.wttech.aem.contentor.core.acl.AclResult;
import com.wttech.aem.contentor.core.acl.utils.AuthorizableManager;
import com.wttech.aem.contentor.core.acl.utils.PermissionsManager;
import com.wttech.aem.contentor.core.util.GroovyUtils;
import groovy.lang.Closure;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.sling.api.resource.ResourceResolver;

public class MyGroup extends MyAuthorizable {

    public MyGroup(
            Authorizable authorizable,
            ResourceResolver resourceResolver,
            AuthorizableManager authorizableManager,
            PermissionsManager permissionsManager,
            boolean compositeNodeStore,
            OutputStream out) {
        super(authorizable, resourceResolver, authorizableManager, permissionsManager, compositeNodeStore, out);
    }

    // TODO Closure accepting methods need to be defined before the simple ones (add arch unit rule to protect it)
    public AclResult with(Closure<MyGroup> closure) {
        AclResult result;
        if (notExists(authorizable)) {
            result = AclResult.SKIPPED;
        } else if (!authorizable.isGroup()) {
            result = AclResult.SKIPPED;
        } else {
            GroovyUtils.with(this, closure);
            result = AclResult.CHANGED;
        }
        logResult("with {}", result);
        return result;
    }

    public AclResult addMember(Closure<MemberOptions> closure) {
        return addMember(GroovyUtils.with(new MemberOptions(), closure));
    }

    public AclResult removeMember(Closure<MemberOptions> closure) {
        return removeMember(GroovyUtils.with(new MemberOptions(), closure));
    }

    public AclResult removeAllMembers(Closure<Void> closure) {
        return removeAllMembers();
    }

    public AclResult purge(Closure<Void> closure) {
        return purge();
    }

    // Non-closure accepting methods

    public AclResult addMember(MemberOptions options) {
        Authorizable member = determineAuthorizable(options.getMember(), options.getMemberId());
        return addMember(member);
    }

    public AclResult addMember(Object memberObj) {
        Authorizable member = determineAuthorizable(memberObj);
        AclResult result;
        if (notExists(authorizable)) {
            result = AclResult.SKIPPED;
        } else if (!authorizable.isGroup()) {
            result = AclResult.SKIPPED;
        } else if (notExists(member)) {
            result = AclResult.SKIPPED;
        } else {
            result = authorizableManager.addMember((Group) authorizable, member) ? AclResult.CHANGED : AclResult.OK;
        }
        logResult("addMember {}", result, getID(member));
        return result;
    }

    public AclResult removeMember(MemberOptions options) {
        Authorizable member = determineAuthorizable(options.getMember(), options.getMemberId());
        return removeMember(member);
    }

    public AclResult removeMember(Object memberObj) {
        Authorizable member = determineAuthorizable(memberObj);
        AclResult result;
        if (notExists(authorizable)) {
            result = AclResult.SKIPPED;
        } else if (!authorizable.isGroup()) {
            result = AclResult.SKIPPED;
        } else if (notExists(member)) {
            result = AclResult.SKIPPED;
        } else {
            result = authorizableManager.removeMember((Group) authorizable, member) ? AclResult.CHANGED : AclResult.OK;
        }
        logResult("removeMember {}", result, getID(member));
        return result;
    }

    public AclResult removeAllMembers() {
        try {
            AclResult result;
            if (notExists(authorizable)) {
                result = AclResult.SKIPPED;
            } else if (!authorizable.isGroup()) {
                result = AclResult.SKIPPED;
            } else {
                Iterator<Authorizable> members = ((Group) authorizable).getMembers();
                result = members.hasNext() ? AclResult.CHANGED : AclResult.OK;
                while (members.hasNext()) {
                    authorizableManager.removeMember((Group) authorizable, members.next());
                }
            }
            logResult("removeAllMembers {}", result);
            return result;
        } catch (RepositoryException e) {
            throw new AclException("Failed to remove all members from group", e);
        }
    }

    public AclResult purge() {
        AclResult result;
        if (notExists(authorizable)) {
            result = AclResult.SKIPPED;
        } else if (!authorizable.isGroup()) {
            result = AclResult.SKIPPED;
        } else {
            result = Arrays.asList(removeAllMembers(), removeFromAllGroups(), clear("/", false))
                            .contains(AclResult.CHANGED)
                    ? AclResult.CHANGED
                    : AclResult.OK;
        }
        logResult("purge {}", result);
        return result;
    }
}
