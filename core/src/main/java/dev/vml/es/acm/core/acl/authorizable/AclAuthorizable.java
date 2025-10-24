package dev.vml.es.acm.core.acl.authorizable;

import dev.vml.es.acm.core.acl.AclContext;
import dev.vml.es.acm.core.acl.AclException;
import dev.vml.es.acm.core.util.GroovyUtils;
import groovy.lang.Closure;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.jcr.RepositoryException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;

public class AclAuthorizable {

    private static final String EVERYONE = "everyone";

    private final Authorizable authorizable;

    private final String id;

    protected final AclContext context;

    public AclAuthorizable(Authorizable authorizable, String id, AclContext context) {
        this.authorizable = authorizable;
        this.id = id;
        this.context = context;
    }

    public void addToGroup(Closure<GroupOptions> closure) {
        addToGroup(GroovyUtils.with(new GroupOptions(), closure));
    }

    public void removeFromGroup(Closure<GroupOptions> closure) {
        removeFromGroup(GroovyUtils.with(new GroupOptions(), closure));
    }

    public void clear(Closure<ClearOptions> closure) {
        clear(GroovyUtils.with(new ClearOptions(), closure));
    }

    public void allow(Closure<PermissionsOptions> closure) {
        allow(GroovyUtils.with(new PermissionsOptions(), closure));
    }

    public void deny(Closure<PermissionsOptions> closure) {
        deny(GroovyUtils.with(new PermissionsOptions(), closure));
    }

    public void setProperty(Closure<SetPropertyOptions> closure) {
        setProperty(GroovyUtils.with(new SetPropertyOptions(), closure));
    }

    public void removeProperty(Closure<RemovePropertyOptions> closure) {
        removeProperty(GroovyUtils.with(new RemovePropertyOptions(), closure));
    }

    public void addToGroup(GroupOptions options) {
        AclGroup group = context.determineGroup(options.getGroup(), options.getGroupId());
        String groupId = context.determineId(options.getGroup(), options.getGroupId());
        
        if (group == null) {
            context.getLogger().info("Skipped adding authorizable '{}' to group '{}' (group not found)", id, groupId);
            return;
        }
        
        boolean changed = context.getAuthorizableManager().addMember(group.get(), authorizable);
        if (changed) {
            context.getLogger().info("Added authorizable '{}' to group '{}'", id, groupId);
        } else {
            context.getLogger().info("Skipped adding authorizable '{}' to group '{}' (already member)", id, groupId);
        }
    }

    public void addToGroup(String groupId) {
        GroupOptions options = new GroupOptions();
        options.setGroupId(groupId);
        addToGroup(options);
    }

    public void addToGroup(AclGroup group) {
        GroupOptions options = new GroupOptions();
        options.setGroup(group);
        addToGroup(options);
    }

    public void removeFromGroup(GroupOptions options) {
        AclGroup group = context.determineGroup(options.getGroup(), options.getGroupId());
        String groupId = context.determineId(options.getGroup(), options.getGroupId());
        
        if (group == null) {
            context.getLogger().info("Skipped removing authorizable '{}' from group '{}' (group not found)", id, groupId);
            return;
        }
        
        boolean changed = context.getAuthorizableManager().removeMember(group.get(), authorizable);
        if (changed) {
            context.getLogger().info("Removed authorizable '{}' from group '{}'", id, groupId);
        } else {
            context.getLogger().info("Skipped removing authorizable '{}' from group '{}' (not a member)", id, groupId);
        }
    }

    public void removeFromGroup(String groupId) {
        GroupOptions options = new GroupOptions();
        options.setGroupId(groupId);
        removeFromGroup(options);
    }

    public void removeFromGroup(AclGroup group) {
        GroupOptions options = new GroupOptions();
        options.setGroup(group);
        removeFromGroup(options);
    }

    public void removeFromAllGroups() {
        try {
            Iterator<Group> groups = authorizable.memberOf();
            boolean anyChanged = false;
            while (groups.hasNext()) {
                Group group = groups.next();
                if (!StringUtils.equals(group.getID(), EVERYONE)
                        && context.getAuthorizableManager().removeMember(group, authorizable)) {
                    anyChanged = true;
                }
            }
            if (anyChanged) {
                context.getLogger().info("Removed authorizable '{}' from all groups", id);
            } else {
                context.getLogger().info("Skipped removing authorizable '{}' from all groups (not a member of any group)", id);
            }
        } catch (RepositoryException e) {
            throw new AclException(String.format("Cannot remove authorizable '%s' from all groups!", id), e);
        }
    }

    public void clear(ClearOptions options) {
        clear(options.getPath(), options.isStrict());
    }

    public void clear(String path, boolean strict) {
        if (context.isCompositeNodeStore() && isAppsOrLibsPath(path)) {
            context.getLogger().info("Skipped clearing permissions for authorizable '{}' at path '{}' (composite node store)", id, path);
            return;
        }
        if (context.getResourceResolver().getResource(path) == null) {
            context.getLogger().info("Skipped clearing permissions for authorizable '{}' at path '{}' (path not found)", id, path);
            return;
        }
        boolean changed = context.getPermissionsManager().clear(authorizable, path, strict);
        if (changed) {
            context.getLogger().info("Cleared permissions for authorizable '{}' at path '{}'", id, path);
        } else {
            context.getLogger().info("Skipped clearing permissions for authorizable '{}' at path '{}' (no permissions to clear)", id, path);
        }
    }

    public void clear(String path) {
        clear(path, false);
    }

    public void purge() {
        context.getLogger().info("Skipped purging authorizable '{}' (operation not supported)", id);
    }

    private void apply(PermissionsOptions options, boolean allow) {
        String path = options.getPath();
        List<String> permissions = options.determineAllPermissions();
        Map<String, Object> restrictions = options.determineAllRestrictions();
        PermissionsOptions.Mode mode = options.getMode();
        
        if (context.isCompositeNodeStore() && isAppsOrLibsPath(path)) {
            String actionDescription = allow ? "allow permissions" : "deny permissions";
            context.getLogger().info("Skipped setting {} for authorizable '{}' at path '{}' (composite node store)", actionDescription, id, path);
            return;
        }
        
        if (context.getResourceResolver().getResource(path) == null) {
            if (mode == PermissionsOptions.Mode.FAIL) {
                throw new AclException(String.format("Cannot apply permissions for authorizable '%s' at path '%s'! (path not found)", id, path));
            }
            String actionDescription = allow ? "allow permissions" : "deny permissions";
            context.getLogger().info("Skipped setting {} for authorizable '{}' at path '{}' (path not found)", actionDescription, id, path);
            return;
        }
        
        if (context.getPermissionsManager().check(authorizable, path, permissions, restrictions, allow)) {
            String actionDescription = allow ? "allow permissions" : "deny permissions";
            context.getLogger().info("Skipped setting {} for authorizable '{}' at path '{}' (already set)", actionDescription, id, path);
        } else {
            context.getPermissionsManager().apply(authorizable, path, permissions, restrictions, allow);
            String actionDescription = allow ? "allow permissions" : "deny permissions";
            context.getLogger().info("Applied {} for authorizable '{}' at path '{}'", actionDescription, id, path);
        }
    }

    public void allow(PermissionsOptions options) {
        apply(options, true);
    }

    public void deny(PermissionsOptions options) {
        apply(options, false);
    }

    public void setProperty(SetPropertyOptions options) {
        setProperty(options.getRelPath(), options.getValue());
    }

    public void setProperty(String relPath, String value) {
        List<String> values = context.getAuthorizableManager().getProperty(authorizable, relPath);
        if (values != null && values.contains(value)) {
            context.getLogger().info("Skipped setting property '{}' for authorizable '{}' (already set)", relPath, id);
        } else {
            context.getAuthorizableManager().setProperty(authorizable, relPath, value);
            context.getLogger().info("Set property '{}' for authorizable '{}'", relPath, id);
        }
    }

    public void removeProperty(RemovePropertyOptions options) {
        removeProperty(options.getRelPath());
    }

    public void removeProperty(String relPath) {
        boolean changed = context.getAuthorizableManager().removeProperty(authorizable, relPath);
        if (changed) {
            context.getLogger().info("Removed property '{}' for authorizable '{}'", relPath, id);
        } else {
            context.getLogger().info("Skipped removing property '{}' for authorizable '{}' (property not set)", relPath, id);
        }
    }

    public Authorizable get() {
        return authorizable;
    }

    public String getId() {
        return id;
    }

    @SuppressWarnings("CQRules:CQBP-71")
    private boolean isAppsOrLibsPath(String path) {
        return StringUtils.startsWithAny(path, "/apps", "/libs");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AclAuthorizable that = (AclAuthorizable) o;
        return new EqualsBuilder().append(id, that.id).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(id).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", id)
                .toString();
    }
}
