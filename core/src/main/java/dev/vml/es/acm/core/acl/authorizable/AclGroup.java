package dev.vml.es.acm.core.acl.authorizable;

import dev.vml.es.acm.core.acl.AclContext;
import dev.vml.es.acm.core.acl.AclException;
import dev.vml.es.acm.core.util.GroovyUtils;
import dev.vml.es.acm.core.util.StreamUtils;
import groovy.lang.Closure;
import java.util.Iterator;
import java.util.stream.Stream;
import javax.jcr.RepositoryException;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;

public class AclGroup extends AclAuthorizable {

    private final Group group;

    public AclGroup(Group group, String id, AclContext context) {
        super(group, id, context);
        this.group = group;
    }

    public void addMember(Closure<MemberOptions> closure) {
        addMember(GroovyUtils.with(new MemberOptions(), closure));
    }

    public void removeMember(Closure<MemberOptions> closure) {
        removeMember(GroovyUtils.with(new MemberOptions(), closure));
    }

    public void addMember(MemberOptions options) {
        AclAuthorizable member = context.determineAuthorizable(options.getMember(), options.getMemberId());
        String memberId = context.determineId(options.getMember(), options.getMemberId());

        if (member == null) {
            context.getLogger().info("Skipped adding member '{}' to group '{}' (member not found)", memberId, getId());
            return;
        }

        boolean changed = context.getAuthorizableManager().addMember(group, member.get());
        if (changed) {
            context.getLogger().info("Added member '{}' to group '{}'", memberId, getId());
        } else {
            context.getLogger().info("Skipped adding member '{}' to group '{}' (already a member)", memberId, getId());
        }
    }

    public void addMember(String memberId) {
        MemberOptions options = new MemberOptions();
        options.setMemberId(memberId);
        addMember(options);
    }

    public void addMember(AclAuthorizable member) {
        MemberOptions options = new MemberOptions();
        options.setMember(member);
        addMember(options);
    }

    public void removeMember(MemberOptions options) {
        AclAuthorizable member = context.determineAuthorizable(options.getMember(), options.getMemberId());
        String memberId = context.determineId(options.getMember(), options.getMemberId());

        if (member == null) {
            context.getLogger()
                    .info("Skipped removing member '{}' from group '{}' (member not found)", memberId, getId());
            return;
        }

        boolean changed = context.getAuthorizableManager().removeMember(group, member.get());
        if (changed) {
            context.getLogger().info("Removed member '{}' from group '{}'", memberId, getId());
        } else {
            context.getLogger().info("Skipped removing member '{}' from group '{}' (not a member)", memberId, getId());
        }
    }

    public void removeMember(String memberId) {
        MemberOptions options = new MemberOptions();
        options.setMemberId(memberId);
        removeMember(options);
    }

    public void removeMember(AclAuthorizable member) {
        MemberOptions options = new MemberOptions();
        options.setMember(member);
        removeMember(options);
    }

    public void removeAllMembers() {
        try {
            Iterator<Authorizable> members = group.getMembers();
            boolean anyChanged = false;
            while (members.hasNext()) {
                context.getAuthorizableManager().removeMember(group, members.next());
                anyChanged = true;
            }
            if (anyChanged) {
                context.getLogger().info("Removed all members from group '{}'", getId());
            } else {
                context.getLogger()
                        .info("Skipped removing all members from group '{}' (no members to remove)", getId());
            }
        } catch (RepositoryException e) {
            throw new AclException(String.format("Cannot remove all members from group '%s'!", getId()), e);
        }
    }

    public Stream<AclAuthorizable> getMembers() {
        try {
            return StreamUtils.asStream(group.getMembers())
                    .map(context::determineAuthorizable)
                    .filter(a -> a != null);
        } catch (RepositoryException e) {
            throw new AclException(String.format("Cannot get members of group '%s'!", getId()), e);
        }
    }

    public Stream<AclGroup> getGroups() {
        try {
            return StreamUtils.asStream(group.getMembers())
                    .filter(g -> g.isGroup())
                    .map(m -> context.determineGroup((Group) m))
                    .filter(g -> g != null);
        } catch (RepositoryException e) {
            throw new AclException(String.format("Cannot get all groups of group '%s'!", getId()), e);
        }
    }

    public Stream<AclUser> getUsers() {
        try {
            return StreamUtils.asStream(group.getMembers())
                    .filter(g -> !g.isGroup())
                    .map(m -> context.determineUser((User) m))
                    .filter(u -> u != null);
        } catch (RepositoryException e) {
            throw new AclException(String.format("Cannot get all users of group '%s'!", getId()), e);
        }
    }

    @Override
    public void purge() {
        removeAllMembers();
        removeFromAllGroups();
        clear("/");
        context.getLogger().info("Purged group '{}'", getId());
    }

    @Override
    public Group get() {
        return group;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AclGroup that = (AclGroup) o;
        return new EqualsBuilder().append(getId(), that.getId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(getId()).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", getId())
                .toString();
    }
}
