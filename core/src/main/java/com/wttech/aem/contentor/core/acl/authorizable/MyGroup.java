package com.wttech.aem.contentor.core.acl.authorizable;

import com.wttech.aem.contentor.core.acl.AclContext;
import com.wttech.aem.contentor.core.acl.AclException;
import com.wttech.aem.contentor.core.acl.AclResult;
import com.wttech.aem.contentor.core.util.GroovyUtils;
import groovy.lang.Closure;
import java.util.Arrays;
import java.util.Iterator;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;

public class MyGroup extends MyAuthorizable {

    private final Group group;

    public MyGroup(Group group, String id, AclContext context) {
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
        MyAuthorizable member = context.determineAuthorizable(options.getMember(), options.getMemberId());
        return addMember(member);
    }

    public AclResult addMember(String memberId) {
        MyAuthorizable member = context.determineAuthorizable(memberId);
        return addMember(member);
    }

    public AclResult addMember(MyAuthorizable member) {
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
        MyAuthorizable member = context.determineAuthorizable(options.getMember(), options.getMemberId());
        return removeMember(member);
    }

    public AclResult removeMember(String memberId) {
        MyAuthorizable member = context.determineAuthorizable(memberId);
        return removeMember(member);
    }

    public AclResult removeMember(MyAuthorizable member) {
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
