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

    public AclResult addMember(MemberOptions options) {
        AclAuthorizable member = context.determineAuthorizable(options.getMember(), options.getMemberId());
        String memberId = context.determineId(options.getMember(), options.getMemberId());
        AclResult result;
        if (member == null) {
            result = AclResult.SKIPPED;
        } else {
            result = context.getAuthorizableManager().addMember(group, member.get()) ? AclResult.CHANGED : AclResult.OK;
        }
        context.getLogger().info("Added member '{}' to group '{}' [{}]", memberId, getId(), result);
        return result;
    }

    public AclResult addMember(String memberId) {
        return addMember(new MemberOptions(null, memberId));
    }

    public AclResult addMember(AclAuthorizable member) {
        return addMember(new MemberOptions(member, null));
    }

    public AclResult removeMember(MemberOptions options) {
        AclAuthorizable member = context.determineAuthorizable(options.getMember(), options.getMemberId());
        String memberId = context.determineId(options.getMember(), options.getMemberId());
        AclResult result;
        if (member == null) {
            result = AclResult.SKIPPED;
        } else {
            result = context.getAuthorizableManager().removeMember(group, member.get())
                    ? AclResult.CHANGED
                    : AclResult.OK;
        }
        context.getLogger().info("Removed member '{}' from group '{}' [{}]", memberId, getId(), result);
        return result;
    }

    public AclResult removeMember(String memberId) {
        return removeMember(new MemberOptions(null, memberId));
    }

    public AclResult removeMember(AclAuthorizable member) {
        return removeMember(new MemberOptions(member, null));
    }

    public AclResult removeAllMembers() {
        try {
            Iterator<Authorizable> members = group.getMembers();
            AclResult result = members.hasNext() ? AclResult.CHANGED : AclResult.OK;
            while (members.hasNext()) {
                context.getAuthorizableManager().removeMember(group, members.next());
            }
            context.getLogger().info("Removed all members from group '{}' [{}]", getId(), result);
            return result;
        } catch (RepositoryException e) {
            throw new AclException(String.format("Failed to remove all members from group '%s'", getId()), e);
        }
    }

    @Override
    public AclResult purge() {
        AclResult result = Arrays.asList(removeAllMembers(), removeFromAllGroups(), clear("/", false))
                        .contains(AclResult.CHANGED)
                ? AclResult.CHANGED
                : AclResult.OK;
        context.getLogger().info("Purged group '{}' [{}]", getId(), result);
        return result;
    }

    @Override
    public Group get() {
        return group;
    }
}
