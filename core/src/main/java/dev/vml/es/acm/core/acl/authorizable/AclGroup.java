package dev.vml.es.acm.core.acl.authorizable;

import dev.vml.es.acm.core.acl.AclContext;
import dev.vml.es.acm.core.acl.AclException;
import dev.vml.es.acm.core.acl.AclResult;
import dev.vml.es.acm.core.util.GroovyUtils;
import dev.vml.es.acm.core.util.StreamUtils;
import groovy.lang.Closure;
import java.util.Iterator;
import java.util.stream.Stream;
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
        MemberOptions options = new MemberOptions();
        options.setMemberId(memberId);
        return addMember(options);
    }

    public AclResult addMember(AclAuthorizable member) {
        MemberOptions options = new MemberOptions();
        options.setMember(member);
        return addMember(options);
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
        MemberOptions options = new MemberOptions();
        options.setMemberId(memberId);
        return removeMember(options);
    }

    public AclResult removeMember(AclAuthorizable member) {
        MemberOptions options = new MemberOptions();
        options.setMember(member);
        return removeMember(options);
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

    public Stream<AclAuthorizable> getMembers() {
        try {
            return StreamUtils.asStream(group.getMembers())
                    .map(member -> {
                        return context.determineAuthorizable(member);
                    })
                    .filter(authorizable -> authorizable != null);
        } catch (RepositoryException e) {
            throw new AclException(String.format("Failed to get members of group '%s'", getId()), e);
        }
    }

    @Override
    public AclResult purge() {
        AclResult result = AclResult.of(removeAllMembers(), removeFromAllGroups(), clear("/"));
        context.getLogger().info("Purged group '{}' [{}]", getId(), result);
        return result;
    }

    @Override
    public Group get() {
        return group;
    }
}
