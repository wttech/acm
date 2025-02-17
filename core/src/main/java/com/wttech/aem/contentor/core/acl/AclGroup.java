package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.MemberOptions;
import com.wttech.aem.contentor.core.util.GroovyUtils;
import groovy.lang.Closure;
import java.util.Arrays;
import java.util.Iterator;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;

public class AclGroup extends AclAuthorizable {

    private final Group group;

    public AclGroup(Group group, String id, AclContext context) {
        super(group, id, context);
        this.group = group;
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

    public AclResult addMember(MemberOptions options) {
        AclAuthorizable member = context.determineAuthorizable(options.getMember(), options.getMemberId());
        return addMember(member);
    }

    public AclResult addMember(String memberId) {
        AclAuthorizable member = context.determineAuthorizable(memberId);
        return addMember(member);
    }

    public AclResult addMember(AclAuthorizable member) {
        AclResult result;
        if (group == null) {
            result = AclResult.SKIPPED;
        } else if (member.get() == null) {
            result = AclResult.SKIPPED;
        } else {
            result = context.getAuthorizableManager().addMember(group, member.get()) ? AclResult.CHANGED : AclResult.OK;
        }
        context.logResult(this, "addMember {} {}", result, member.getId());
        return result;
    }

    public AclResult removeMember(MemberOptions options) {
        AclAuthorizable member = context.determineAuthorizable(options.getMember(), options.getMemberId());
        return removeMember(member);
    }

    public AclResult removeMember(String memberId) {
        AclAuthorizable member = context.determineAuthorizable(memberId);
        return removeMember(member);
    }

    public AclResult removeMember(AclAuthorizable member) {
        AclResult result;
        if (group == null) {
            result = AclResult.SKIPPED;
        } else if (member.get() == null) {
            result = AclResult.SKIPPED;
        } else {
            result = context.getAuthorizableManager().removeMember(group, member.get())
                    ? AclResult.CHANGED
                    : AclResult.OK;
        }
        context.logResult(this, "removeMember {} {}", result, member.getId());
        return result;
    }

    public AclResult removeAllMembers() {
        try {
            AclResult result;
            if (group == null) {
                result = AclResult.SKIPPED;
            } else {
                Iterator<Authorizable> members = group.getMembers();
                result = members.hasNext() ? AclResult.CHANGED : AclResult.OK;
                while (members.hasNext()) {
                    context.getAuthorizableManager().removeMember(group, members.next());
                }
            }
            context.logResult(this, "removeAllMembers {}", result);
            return result;
        } catch (RepositoryException e) {
            throw new AclException("Failed to remove all members from group", e);
        }
    }

    @Override
    public AclResult purge() {
        AclResult result;
        if (group == null) {
            result = AclResult.SKIPPED;
        } else {
            result = Arrays.asList(removeAllMembers(), removeFromAllGroups(), clear("/", false))
                            .contains(AclResult.CHANGED)
                    ? AclResult.CHANGED
                    : AclResult.OK;
        }
        context.logResult(this, "purge {}", result);
        return result;
    }

    @Override
    public Group get() {
        return group;
    }
}
