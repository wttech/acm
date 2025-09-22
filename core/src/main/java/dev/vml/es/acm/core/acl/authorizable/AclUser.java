package dev.vml.es.acm.core.acl.authorizable;

import dev.vml.es.acm.core.acl.AclContext;
import dev.vml.es.acm.core.acl.AclResult;
import dev.vml.es.acm.core.util.GroovyUtils;
import groovy.lang.Closure;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.jackrabbit.api.security.user.User;

public class AclUser extends AclAuthorizable {

    private final User user;

    public AclUser(User user, String id, AclContext context) {
        super(user, id, context);
        this.user = user;
    }

    public AclResult setPassword(Closure<PasswordOptions> closure) {
        return setPassword(GroovyUtils.with(new PasswordOptions(), closure));
    }

    @Override
    public AclResult purge() {
        AclResult result = AclResult.of(removeFromAllGroups(), clear("/"));
        context.getLogger().info("Purged user '{}' [{}]", getId(), result);
        return result;
    }

    public AclResult setPassword(PasswordOptions options) {
        return setPassword(options.getPassword());
    }

    public AclResult setPassword(String password) {
        AclResult result;
        if (context.getAuthorizableManager().testPassword(user, password)) {
            result = AclResult.OK;
        } else {
            context.getAuthorizableManager().changePassword(user, password);
            result = AclResult.CHANGED;
        }
        context.getLogger().info("Set password for user '{}' [{}]", getId(), result);
        return result;
    }

    @Override
    public User get() {
        return user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AclUser that = (AclUser) o;
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
