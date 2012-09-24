/*
 * Copyright 2011 ZerothAngel <zerothangel@tyrannyofheaven.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tyrannyofheaven.bukkit.zPermissions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.tyrannyofheaven.bukkit.util.ToHLoggingUtils;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;

/**
 * Responsible for resolving a player's effective permissions.
 * 
 * @author zerothangel
 */
public class PermissionsResolver {

    private final ZPermissionsPlugin plugin;

    private final PermissionDao dao;

    private final Set<String> groupPermissionFormats = new HashSet<String>();

    private final Set<String> assignedGroupPermissionFormats = new HashSet<String>();

    private String defaultGroup;

    private boolean includeDefaultInAssigned = true;

    // For plugin use
    PermissionsResolver(ZPermissionsPlugin plugin) {
        this.plugin = plugin;
        this.dao = null;
    }

    // For testing
    public PermissionsResolver(PermissionDao dao) {
        this.plugin = null;
        this.dao = dao;
    }

    /**
     * Set group permission format strings.
     * 
     * @param groupPermissionFormats the group permission format strings,
     *   suitable for use with {@link String#format(String, Object...)}
     */
    public void setGroupPermissionFormats(Collection<String> groupPermissionFormats) {
        this.groupPermissionFormats.clear();
        if (groupPermissionFormats != null)
            this.groupPermissionFormats.addAll(groupPermissionFormats);
    }

    /**
     * Set group permission format strings for assigned groups.
     * 
     * @param assignedGroupPermissionFormats the group permission format strings,
     *   suitable for use with {@link String#format(String, Object...)}
     */
    public void setAssignedGroupPermissionFormats(Collection<String> assignedGroupPermissionFormats) {
        this.assignedGroupPermissionFormats.clear();
        if (assignedGroupPermissionFormats != null)
            this.assignedGroupPermissionFormats.addAll(assignedGroupPermissionFormats);
    }

    /**
     * Set name of the default group.
     * 
     * @param defaultGroup name of the default group
     */
    public void setDefaultGroup(String defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    // Get DAO, accounting for decoupling from plugin
    private PermissionDao getDao() {
        return plugin == null ? dao : plugin.getDao();
    }

    // Get group permission format strings
    private Set<String> getGroupPermissionFormats() {
        return groupPermissionFormats;
    }

    // Get assigned group permission format strings
    private Set<String> getAssignedGroupPermissionFormats() {
        return assignedGroupPermissionFormats;
    }

    /**
     * Retrieve the configured default group.
     * 
     * @return the default group
     */
    public String getDefaultGroup() {
        return defaultGroup;
    }

    // Returns whether or not default group should be included in assigned permissions
    private boolean isIncludeDefaultInAssigned() {
        return includeDefaultInAssigned;
    }

    /**
     * Sets whether not the default group should be included in assigned permissions.
     * If false, the default group will never be listed as an assigned permission,
     * even if it is explicitly assigned.
     * 
     * @param includeDefaultInAssigned false if the default group should never be included in assigned permissions
     */
    public void setIncludeDefaultInAssigned(boolean includeDefaultInAssigned) {
        this.includeDefaultInAssigned = includeDefaultInAssigned;
    }

    // Output debug message
    private void debug(String format, Object... args) {
        if (plugin == null)
            Logger.getLogger(getClass().getName()).info(String.format(format, args));
        else
            ToHLoggingUtils.debug(plugin, format, args);
    }

    /**
     * Resolve a player's permissions. Any permissions declared on the player
     * should override group permissions.
     * NB: world and regions should all be in lowercase!
     * 
     * @param playerName the player's name
     * @param world the desination world name in lowercase
     * @param regions the name of the regions containing the destination, all
     *   in lowercase
     * @return effective permissions for this player
     */
    public Map<String, Boolean> resolvePlayer(String playerName, String world, Set<String> regions) {
        // Get this player's groups
        List<String> groups = getDao().getGroups(playerName);
        if (groups.isEmpty()) {
            // If no groups, use the default group
            groups.add(getDefaultGroup());
        }
 
        List<Entry> entries = new ArrayList<Entry>();
    
        // Resolve each group in turn (highest priority resolved last)
        debug("Groups for %s: %s", playerName, groups);
        for (String group : groups) {
            resolveGroup(entries, group);
        }
    
        // Player-specific permissions overrides all group permissions
        entries.addAll(getDao().getEntries(playerName, false));
    
        return applyPermissions(entries, regions, world);
    }

    /**
     * Resolve a group's permissions. The permissions from the group's furthest
     * ancestor are applied first, followed by each succeeding ancestor. (And
     * finally ending with the group itself.)
     * 
     * @param groupName the group's name
     * @param world the destination world name in lowercase
     * @param regions the name of the regions containing the destination, all
     *   in lowercase
     * @return effective permissions for this group
     */
    public Map<String, Boolean> resolveGroup(String groupName, String world, Set<String> regions) {
        List<Entry> entries = new ArrayList<Entry>();
        resolveGroup(entries, groupName);
        return applyPermissions(entries, regions, world);
    }

    // Resolve a group's permissions. Ancestor permissions should be overridden
    // by each successive descendant.
    private void resolveGroup(List<Entry> entries, String group) {
        List<String> ancestry = getDao().getAncestry(group);
        if (ancestry.isEmpty()) {
            // This only happens when the default group does not exist
            ancestry.add(getDefaultGroup());
        }
        debug("Ancestry for %s: %s", group, ancestry);
        
        if (!getDefaultGroup().equalsIgnoreCase(group) || isIncludeDefaultInAssigned()) {
            // Add assigned group permissions, if present
            for (String groupPermissionFormat : getAssignedGroupPermissionFormats()) {
                Entry groupPerm = new Entry();
                groupPerm.setPermission(String.format(groupPermissionFormat, group));
                groupPerm.setValue(true);
                entries.add(groupPerm);
            }
        }

        // Apply permission from each ancestor
        for (String ancestor : ancestry) {
            // Add group permissions, if present
            for (String groupPermissionFormat : getGroupPermissionFormats()) {
                Entry groupPerm = new Entry();
                groupPerm.setPermission(String.format(groupPermissionFormat, ancestor));
                groupPerm.setValue(true);
                entries.add(groupPerm);
            }

            entries.addAll(getDao().getEntries(ancestor, true)); // WHYYYY
        }
    }

    // Apply an entity's permissions to the permission map. Universal permissions
    // (ones not assigned to any specific world) are applied first. They are
    // then overridden by any world-specific permissions.
    private Map<String, Boolean> applyPermissions(List<Entry> entries, Set<String> regions, String world) {
        Map<String, Boolean> permissions = new LinkedHashMap<String, Boolean>();

        Map<String, Boolean> regionPermissions = new LinkedHashMap<String, Boolean>();
        List<Entry> worldPermissions = new ArrayList<Entry>();

        // Apply non-region-specific, non-world-specific permissions first
        for (Entry e : entries) {
            if (e.getRegion() == null && e.getWorld() == null) {
                permissions.put(e.getPermission(), e.isValue());
            }
            else if (e.getRegion() != null && e.getWorld() == null) {
                // Universal region-specific (should these really be supported?)
                if (regions.contains(e.getRegion().getName()))
                    regionPermissions.put(e.getPermission(), e.isValue());
            }
            else if (e.getWorld().getName().equals(world)) {
                worldPermissions.add(e);
            }
        }

        // Then override with world-specific permissions
        Map<String, Boolean> regionWorldPermissions = new HashMap<String, Boolean>();
        for (Entry e : worldPermissions) {
            if (e.getRegion() == null) {
                // Non region-specific
                permissions.put(e.getPermission(), e.isValue());
            }
            else {
                if (regions.contains(e.getRegion().getName()))
                    regionWorldPermissions.put(e.getPermission(), e.isValue());
            }
        }
        
        // Override with universal, region-specific permissions
        permissions.putAll(regionPermissions);

        // Finally, override with region- and world-specific permissions
        permissions.putAll(regionWorldPermissions);
        
        return permissions;
    }

}
