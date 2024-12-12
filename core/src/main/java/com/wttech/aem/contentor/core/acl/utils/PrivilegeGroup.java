package com.wttech.aem.contentor.core.acl.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import org.apache.commons.lang3.StringUtils;

public enum PrivilegeGroup {

    READ("READ", Privilege.JCR_READ),

    MODIFY("MODIFY",
            Privilege.JCR_MODIFY_PROPERTIES,
            Privilege.JCR_LOCK_MANAGEMENT,
            Privilege.JCR_VERSION_MANAGEMENT),

    CREATE("CREATE",
            Privilege.JCR_ADD_CHILD_NODES,
            Privilege.JCR_NODE_TYPE_MANAGEMENT),

    DELETE("DELETE",
            Privilege.JCR_REMOVE_NODE,
            Privilege.JCR_REMOVE_CHILD_NODES),

    READ_ACL("READ_ACL", Privilege.JCR_READ_ACCESS_CONTROL),

    MODIFY_ACL("MODIFY_ACL", Privilege.JCR_MODIFY_ACCESS_CONTROL),

    REPLICATE("REPLICATE", "crx:replicate"),

    ALL("ALL",
            Privilege.JCR_READ,
            Privilege.JCR_WRITE,
            Privilege.JCR_LOCK_MANAGEMENT,
            Privilege.JCR_VERSION_MANAGEMENT,
            Privilege.JCR_NODE_TYPE_MANAGEMENT,
            "crx:replicate"),

    MODIFY_PAGE("MODIFY_PAGE",
            Privilege.JCR_REMOVE_NODE,
            Privilege.JCR_REMOVE_CHILD_NODES,
            Privilege.JCR_NODE_TYPE_MANAGEMENT,
            Privilege.JCR_ADD_CHILD_NODES),

    DELETE_CHILD_NODES("DELETE_CHILD_NODES", Privilege.JCR_REMOVE_CHILD_NODES);

    private final String title;

    private final List<String> privileges;

    PrivilegeGroup(String title, String... privileges) {
        this.title = title;
        this.privileges = Arrays.asList(privileges);
    }

    public static PrivilegeGroup fromTitle(String title) {
        return Arrays.stream(PrivilegeGroup.values())
                .filter(item -> StringUtils.equalsIgnoreCase(title, item.getTitle()))
                .findFirst()
                .orElse(null);
    }

    public String getTitle() {
        return title;
    }

    public List<Privilege> toPrivileges(AccessControlManager accessControlManager)
            throws RepositoryException {
        List<Privilege> result = new ArrayList<>();
        for (String privilege : privileges) {
            result.add(accessControlManager.privilegeFromName(privilege));
        }
        return result;
    }
}
