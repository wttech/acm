package com.wttech.aem.contentor.core.acl.authorizable;

import com.wttech.aem.contentor.core.acl.AclContext;
import com.wttech.aem.contentor.core.acl.AclResult;
import com.wttech.aem.contentor.core.util.GroovyUtils;
import groovy.lang.Closure;
import java.util.Arrays;
import org.apache.jackrabbit.api.security.user.User;

public class MyUser extends MyAuthorizable {

    private final User user;

    public MyUser(User user, String id, AclContext context) {
        super(user, id, context);
        this.user = user;
    }

    public AclResult setPassword(Closure<PasswordOptions> closure) {
        return setPassword(GroovyUtils.with(new PasswordOptions(), closure));
    }

    @Override
    public AclResult purge() {
        AclResult result;
        if (user == null) {
            result = AclResult.SKIPPED;
        } else {
            result = Arrays.asList(removeFromAllGroups(), clear("/", false)).contains(AclResult.CHANGED)
                    ? AclResult.CHANGED
                    : AclResult.OK;
        }
        context.logResult(this, "purge {}", result);
        return result;
    }

    public AclResult setPassword(PasswordOptions options) {
        return setPassword(options.getPassword());
    }

    public AclResult setPassword(String password) {
        AclResult result;
        if (user == null) {
            result = AclResult.SKIPPED;
        } else if (context.getAuthorizableManager().testPassword(user, password)) {
            result = AclResult.OK;
        } else {
            context.getAuthorizableManager().changePassword(user, password);
            result = AclResult.CHANGED;
        }
        context.logResult(this, "setPassword {}", result);
        return result;
    }

    @Override
    public User get() {
        return user;
    }
}
