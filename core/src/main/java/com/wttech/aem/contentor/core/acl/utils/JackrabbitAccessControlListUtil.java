package com.wttech.aem.contentor.core.acl.utils;

import com.wttech.aem.contentor.core.acl.AclException;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;

public final class JackrabbitAccessControlListUtil {

    private JackrabbitAccessControlListUtil() {
        // intentionally empty
    }

    public static JackrabbitAccessControlList determineModifiableAcl(AccessControlManager accessManager, String path)
            throws RepositoryException {
        JackrabbitAccessControlList acl = determineAcl(accessManager, path);
        if (acl != null) {
            return acl;
        }
        JackrabbitAccessControlList applicableAcl = determineApplicableAcl(accessManager, path);
        if (applicableAcl != null) {
            return applicableAcl;
        }
        throw new AclException("Failed to determine modifiable ACL at " + path);
    }

    private static JackrabbitAccessControlList determineAcl(AccessControlManager accessManager, String path)
            throws RepositoryException {
        AccessControlPolicy[] policies = accessManager.getPolicies(path);
        for (AccessControlPolicy policy : policies) {
            if (policy instanceof JackrabbitAccessControlList) {
                return (JackrabbitAccessControlList) policy;
            }
        }
        return null;
    }

    private static JackrabbitAccessControlList determineApplicableAcl(AccessControlManager accessManager, String path)
            throws RepositoryException {
        AccessControlPolicyIterator policies = accessManager.getApplicablePolicies(path);
        while (policies.hasNext()) {
            AccessControlPolicy policy = policies.nextAccessControlPolicy();
            if (policy instanceof JackrabbitAccessControlList) {
                return (JackrabbitAccessControlList) policy;
            }
        }
        return null;
    }
}
