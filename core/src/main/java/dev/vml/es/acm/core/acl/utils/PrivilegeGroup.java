package dev.vml.es.acm.core.acl.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.jcr.security.Privilege;
import org.apache.commons.lang3.StringUtils;

public enum PrivilegeGroup {
    READ("READ", Privilege.JCR_READ),
    MODIFY("MODIFY", Privilege.JCR_MODIFY_PROPERTIES, Privilege.JCR_LOCK_MANAGEMENT, Privilege.JCR_VERSION_MANAGEMENT),
    CREATE("CREATE", Privilege.JCR_ADD_CHILD_NODES, Privilege.JCR_NODE_TYPE_MANAGEMENT),
    DELETE("DELETE", Privilege.JCR_REMOVE_NODE, Privilege.JCR_REMOVE_CHILD_NODES),
    READ_ACL("READ_ACL", Privilege.JCR_READ_ACCESS_CONTROL),
    MODIFY_ACL("MODIFY_ACL", Privilege.JCR_MODIFY_ACCESS_CONTROL),
    REPLICATE("REPLICATE", "crx:replicate"),
    ALL(
            "ALL",
            Privilege.JCR_READ,
            Privilege.JCR_WRITE,
            Privilege.JCR_LOCK_MANAGEMENT,
            Privilege.JCR_VERSION_MANAGEMENT,
            Privilege.JCR_NODE_TYPE_MANAGEMENT,
            "crx:replicate"),
    MODIFY_PAGE(
            "MODIFY_PAGE",
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

    public static List<String> determinePermissions(String title) {
        return Arrays.stream(PrivilegeGroup.values())
                .filter(item -> StringUtils.equalsIgnoreCase(title, item.title))
                .findFirst()
                .map(item -> item.privileges)
                .orElse(Collections.singletonList(title));
    }
}
