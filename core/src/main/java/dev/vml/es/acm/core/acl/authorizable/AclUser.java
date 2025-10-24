package dev.vml.es.acm.core.acl.authorizable;

import dev.vml.es.acm.core.acl.AclContext;
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

    public void setPassword(Closure<PasswordOptions> closure) {
        setPassword(GroovyUtils.with(new PasswordOptions(), closure));
    }

    @Override
    public void purge() {
        removeFromAllGroups();
        clear("/");
        context.getLogger().info("Purged user '{}'", getId());
    }

    public void setPassword(PasswordOptions options) {
        setPassword(options.getPassword());
    }

    public void setPassword(String password) {
        if (context.getAuthorizableManager().testPassword(user, password)) {
            context.getLogger().info("Password already set for user '{}'", getId());
        } else {
            context.getAuthorizableManager().changePassword(user, password);
            context.getLogger().info("Set password for user '{}'", getId());
        }
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
