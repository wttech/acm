package dev.vml.es.acm.core.acl.utils;

import dev.vml.es.acm.core.acl.AclException;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;

public final class JcrAclUtils {

    private JcrAclUtils() {
        // intentionally empty
    }

    public static JackrabbitAccessControlList determineModifiableAcl(
            AccessControlManager accessControlManager, String path) {
        JackrabbitAccessControlList acl = determineAcl(accessControlManager, path);
        if (acl != null) {
            return acl;
        }
        JackrabbitAccessControlList applicableAcl = determineApplicableAcl(accessControlManager, path);
        if (applicableAcl != null) {
            return applicableAcl;
        }
        throw new AclException(String.format("Failed to determine modifiable ACL at path '%s'", path));
    }

    private static JackrabbitAccessControlList determineAcl(AccessControlManager accessControlManager, String path) {
        try {
            AccessControlPolicy[] policies = accessControlManager.getPolicies(path);
            for (AccessControlPolicy policy : policies) {
                if (policy instanceof JackrabbitAccessControlList) {
                    return (JackrabbitAccessControlList) policy;
                }
            }
            return null;
        } catch (RepositoryException e) {
            throw new AclException(String.format("Failed to determine ACL at path '%s'", path));
        }
    }

    private static JackrabbitAccessControlList determineApplicableAcl(
            AccessControlManager accessControlManager, String path) {
        try {
            AccessControlPolicyIterator policies = accessControlManager.getApplicablePolicies(path);
            while (policies.hasNext()) {
                AccessControlPolicy policy = policies.nextAccessControlPolicy();
                if (policy instanceof JackrabbitAccessControlList) {
                    return (JackrabbitAccessControlList) policy;
                }
            }
            return null;
        } catch (RepositoryException e) {
            throw new AclException(String.format("Failed to determine applicable ACL at path '%s'", path));
        }
    }
}
