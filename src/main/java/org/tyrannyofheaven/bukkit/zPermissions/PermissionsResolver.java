/*
 * Copyright 2011 Allan Saddi <allan@saddi.com>
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;

/**
 * Responsible for resolving a player's effective permissions.
 * 
 * @author asaddi
 */
public class PermissionsResolver {

    private final ZPermissionsPlugin plugin;

    private final PermissionDao dao;

    private String groupPermissionFormat;

    private String defaultGroup;

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
     * Set group permission format string.
     * 
     * @param groupPermissionFormat the group permission format string,
     *   suitable for use with {@link String#format(String, Object...)}
     */
    public void setGroupPermissionFormat(String groupPermissionFormat) {
        this.groupPermissionFormat = groupPermissionFormat;
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

    // Get group permission format string
    private String getGroupPermissionFormat() {
        return groupPermissionFormat;
    }

    /**
     * Retrieve the configured default group.
     * 
     * @return the default group
     */
    String getDefaultGroup() {
        return defaultGroup;
    }

    // Output debug message
    private void debug(String format, Object... args) {
        if (plugin == null)
            Logger.getLogger(getClass().getName()).info(String.format(format, args));
        else
            plugin.debug(format, args);
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
    
        Map<String, Boolean> permissions = new HashMap<String, Boolean>();
    
        // Resolve each group in turn (highest priority resolved last)
        debug("Groups for %s: %s", playerName, groups);
        for (String group : groups) {
            resolveGroup(permissions, regions, world, group);
        }
    
        // Player-specific permissions overrides all group permissions
        applyPermissions(permissions, playerName, false, regions, world);
    
        return permissions;
    }

    // Resolve a group's permissions. Ancestor permissions should be overridden
    // by each successive descendant.
    private void resolveGroup(Map<String, Boolean> permissions, Set<String> regions, String world, String group) {
        List<String> ancestry = getDao().getAncestry(group);

        debug("Ancestry for %s: %s", group, ancestry);
        for (String ancestor : ancestry) {
            applyPermissions(permissions, ancestor, true, regions, world);
            
            // Add group permission, if present
            if (getGroupPermissionFormat() != null) {
                permissions.put(String.format(getGroupPermissionFormat(), ancestor), Boolean.TRUE);
            }
        }
    }

    // Apply an entity's permissions to the permission map. Universal permissions
    // (ones not assigned to any specific world) are applied first. They are
    // then overridden by any world-specific permissions.
    private void applyPermissions(Map<String, Boolean> permissions, String name, boolean group, Set<String> regions, String world) {
        Map<String, Boolean> regionPermissions = new HashMap<String, Boolean>();
        List<Entry> worldPermissions = new ArrayList<Entry>();

        // Apply non-region-specific, non-world-specific permissions first
        for (Entry e : getDao().getEntries(name, group)) { // WHYYY
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
    }

}
