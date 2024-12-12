package com.wttech.aem.contentor.core.acl.utils;

import com.wttech.aem.contentor.core.acl.AclException;
import javax.jcr.Node;
import javax.jcr.Session;

public final class RuntimeUtils {

    private RuntimeUtils() {
        // intentionally empty
    }

    public static boolean determineCompositeNodeStore(Session session) {
        try {
            Node node = session.getNode("/apps");
            boolean hasPermission = session.hasPermission("/", Session.ACTION_SET_PROPERTY);
            boolean hasCapability = session.hasCapability("addNode", node, new Object[]{"nt:folder"});
            return hasPermission && !hasCapability;
        } catch (Exception e) {
            throw new AclException("Failed to check if session is connected to a composite node store", e);
        }
    }
}
