package com.wttech.aem.contentor.core.acl.authorizable;

import com.wttech.aem.contentor.core.acl.AclResult;
import com.wttech.aem.contentor.core.acl.utils.AuthorizableManager;
import com.wttech.aem.contentor.core.acl.utils.PermissionsManager;
import com.wttech.aem.contentor.core.util.GroovyUtils;
import groovy.lang.Closure;
import java.io.OutputStream;
import java.util.Arrays;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.resource.ResourceResolver;

public class MyUser extends MyAuthorizable {

    public MyUser(
            Authorizable authorizable,
            ResourceResolver resourceResolver,
            AuthorizableManager authorizableManager,
            PermissionsManager permissionsManager,
            boolean compositeNodeStore,
            OutputStream out) {
        super(authorizable, resourceResolver, authorizableManager, permissionsManager, compositeNodeStore, out);
    }

    // TODO Closure accepting methods need to be defined before the simple ones (add arch unit rule to protect it)
    public AclResult with(Closure<MyUser> closure) {
        AclResult result;
        if (notExists(authorizable)) {
            result = AclResult.SKIPPED;
        } else if (authorizable.isGroup()) {
            result = AclResult.SKIPPED;
        } else {
            GroovyUtils.with(this, closure);
            result = AclResult.CHANGED;
        }
        logResult("with {}", result);
        return result;
    }

    public AclResult purge(Closure<Void> closure) {
        return purge();
    }

    public AclResult setPassword(Closure<PasswordOptions> closure) {
        return setPassword(GroovyUtils.with(new PasswordOptions(), closure));
    }

    // Non-closure accepting methods

    public AclResult purge() {
        AclResult result;
        if (notExists(authorizable)) {
            result = AclResult.SKIPPED;
        } else if (authorizable.isGroup()) {
            result = AclResult.SKIPPED;
        } else {
            result = Arrays.asList(removeFromAllGroups(), clear("/", false)).contains(AclResult.CHANGED)
                    ? AclResult.CHANGED
                    : AclResult.OK;
        }
        logResult("purge {}", result);
        return result;
    }

    public AclResult setPassword(PasswordOptions options) {
        return setPassword(options.getPassword());
    }

    public AclResult setPassword(String password) {
        AclResult result;
        if (notExists(authorizable)) {
            result = AclResult.SKIPPED;
        } else if (authorizable.isGroup()) {
            result = AclResult.SKIPPED;
        } else if (authorizableManager.testPassword(authorizable, password)) {
            result = AclResult.OK;
        } else {
            authorizableManager.changePassword((User) authorizable, password);
            result = AclResult.CHANGED;
        }
        logResult("setPassword {}", result);
        return result;
    }
}
