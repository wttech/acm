package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.utils.AuthorizableManager;
import java.io.IOException;
import java.io.OutputStream;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlManager;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.helpers.MessageFormatter;

public class Context {

    private final JackrabbitSession session;

    private final AuthorizableManager authorizableManager;

    private final AccessControlManager accessControlManager;

    private final OutputStream out;

    public Context(ResourceResolver resourceResolver, OutputStream out) throws AclException {
        try {
            this.session = (JackrabbitSession) resourceResolver.adaptTo(Session.class);
            UserManager userManager = session.getUserManager();
            ValueFactory valueFactory = session.getValueFactory();
            this.authorizableManager = new AuthorizableManager(userManager, valueFactory);
            this.accessControlManager = session.getAccessControlManager();
        } catch (RepositoryException e) {
            throw new AclException(e);
        }
        this.out = out;
    }

    public JackrabbitSession getSession() {
        return session;
    }

    public AuthorizableManager getAuthorizableManager() {
        return authorizableManager;
    }

    private AccessControlManager getAccessControlManager() {
        return accessControlManager;
    }

    public void logMessage(String messagePattern, Object... args) {
        try {
            String message = MessageFormatter.format(messagePattern, args).getMessage();
            out.write(message.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
