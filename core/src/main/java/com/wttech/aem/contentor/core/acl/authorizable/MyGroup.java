package com.wttech.aem.contentor.core.acl.authorizable;

import com.wttech.aem.contentor.core.acl.AclException;
import com.wttech.aem.contentor.core.acl.AclResult;
import com.wttech.aem.contentor.core.acl.utils.AuthorizableManager;
import com.wttech.aem.contentor.core.acl.utils.PermissionsManager;
import com.wttech.aem.contentor.core.util.GroovyUtils;
import groovy.lang.Closure;
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
            boolean compositeNodeStore) {
        super(authorizable, resourceResolver, authorizableManager, permissionsManager, compositeNodeStore);
    }

    // TODO Closure accepting methods need to be defined before the simple ones (add arch unit rule to protect it)
    public AclResult addMember(Closure<MemberOptions> closure) {
        return addMember(GroovyUtils.with(new MemberOptions(), closure));
    }

    public AclResult removeMember(Closure<MemberOptions> closure) {
        return removeMember(GroovyUtils.with(new MemberOptions(), closure));
    }

    public AclResult removeAllMembers(Closure<EmptyOptions> closure) {
        return removeAllMembers();
    }

    public AclResult purge(Closure<EmptyOptions> closure) {
        return purge();
    }

    // Non-closure accepting methods

    public AclResult addMember(MemberOptions options) {
        Authorizable member = determineAuthorizable(options.getMember(), options.getMemberId());
        return addMember(member);
    }

    public AclResult addMember(Authorizable member) {
        AclResult result;
        if (notExists(authorizable)) {
            result = AclResult.SKIPPED;
        } else if (!authorizable.isGroup()) {
            result = AclResult.SKIPPED;
        } else if (notExists(member)) {
            result = AclResult.SKIPPED;
        } else {
            result = authorizableManager.addMember((Group) authorizable, member)
                    ? AclResult.DONE
                    : AclResult.ALREADY_DONE;
        }
        return result;
    }

    public AclResult addMember(String memberId) {
        Authorizable member = determineAuthorizable(memberId);
        return addMember(member);
    }

    public AclResult removeMember(MemberOptions options) {
        Authorizable member = determineAuthorizable(options.getMember(), options.getMemberId());
        return removeMember(member);
    }

    public AclResult removeMember(Authorizable member) {
        AclResult result;
        if (notExists(authorizable)) {
            result = AclResult.SKIPPED;
        } else if (!authorizable.isGroup()) {
            result = AclResult.SKIPPED;
        } else if (notExists(member)) {
            result = AclResult.SKIPPED;
        } else {
            result = authorizableManager.removeMember((Group) authorizable, member)
                    ? AclResult.DONE
                    : AclResult.ALREADY_DONE;
        }
        return result;
    }

    public AclResult removeMember(String memberId) {
        Authorizable member = determineAuthorizable(memberId);
        return removeMember(member);
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
                result = members.hasNext() ? AclResult.DONE : AclResult.ALREADY_DONE;
                while (members.hasNext()) {
                    authorizableManager.removeMember((Group) authorizable, members.next());
                }
            }
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
            result = AclResult.ALREADY_DONE;
            if (removeAllMembers() != AclResult.ALREADY_DONE) {
                result = AclResult.DONE;
            }
            if (removeFromAllGroups() != AclResult.ALREADY_DONE) {
                result = AclResult.DONE;
            }
            if (clear("/", false) != AclResult.ALREADY_DONE) {
                result = AclResult.DONE;
            }
        }
        return result;
    }
}
