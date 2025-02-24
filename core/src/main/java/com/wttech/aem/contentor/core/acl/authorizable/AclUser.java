package com.wttech.aem.contentor.core.acl.authorizable;

import com.wttech.aem.contentor.core.acl.AclContext;
import com.wttech.aem.contentor.core.acl.AclResult;
import com.wttech.aem.contentor.core.util.GroovyUtils;
import groovy.lang.Closure;
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
}
