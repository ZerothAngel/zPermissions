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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tyrannyofheaven.bukkit.util.ToHLoggingUtils;
import org.tyrannyofheaven.bukkit.util.uuid.UuidUtils;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionService;
import org.tyrannyofheaven.bukkit.zPermissions.model.EntityMetadata;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.util.GlobPattern;
import org.tyrannyofheaven.bukkit.zPermissions.util.Utils;

/**
 * Responsible for resolving a player's effective permissions.
 * 
 * @author zerothangel
 */
public class PermissionsResolver {

    private static final Object NULL_ALIAS = new Object();

    private final ZPermissionsPlugin plugin;

    private final PermissionService permissionService;

    private final Set<String> groupPermissionFormats = new HashSet<>();

    private final Set<String> assignedGroupPermissionFormats = new HashSet<>();

    private String defaultGroup;

    private boolean opaqueInheritance = true;

    private boolean interleavedPlayerPermissions = true;

    private boolean includeDefaultInAssigned = true;

    private final Map<String, List<Pattern>> worldAliases = new LinkedHashMap<>(); // target -> [world1, world2, ...]

    private final Map<String, Object> worldAliasCache = new HashMap<>(); // world -> target

    // For plugin use
    PermissionsResolver(ZPermissionsPlugin plugin) {
        this.plugin = plugin;
        this.permissionService = null;
    }

    // For testing
    public PermissionsResolver(PermissionService permissionService) {
        this.plugin = null;
        this.permissionService = permissionService;
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

    // Get PermissionService, accounting for decoupling from plugin
    private PermissionService getPermissionService() {
        return plugin == null ? permissionService : plugin.getPermissionService();
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

    private boolean isOpaqueInheritance() {
        return opaqueInheritance;
    }

    public void setOpaqueInheritance(boolean opaqueInheritance) {
        this.opaqueInheritance = opaqueInheritance;
    }

    private boolean isInterleavedPlayerPermissions() {
        return interleavedPlayerPermissions;
    }

    public void setInterleavedPlayerPermissions(boolean interleavedPlayerPermissions) {
        this.interleavedPlayerPermissions = interleavedPlayerPermissions;
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

    public void addWorldAlias(String world, String target) {
        target = target.toLowerCase();
        List<Pattern> patterns = worldAliases.get(target);
        if (patterns == null) {
            patterns = new ArrayList<>();
            worldAliases.put(target, patterns);
        }
        patterns.add(GlobPattern.compile(world.toLowerCase()));
    }

    public void clearWorldAliases() {
        worldAliases.clear();
        worldAliasCache.clear();
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
     * @param uuid TODO
     * @param world the desination world name in lowercase or null
     * @param regions the name of the regions containing the destination, all
     *   in lowercase
     * @param playerName the player's name
     * @return effective permissions for this player
     */
    public ResolverResult resolvePlayer(UUID uuid, String world, Set<String> regions) {
        String playerName = UuidUtils.canonicalizeUuid(uuid);
        // Get this player's groups
        List<String> groups = Utils.toGroupNames(Utils.filterExpired(getPermissionService().getGroups(uuid)));
        if (groups.isEmpty()) {
            // If no groups, use the default group
            groups.add(getDefaultGroup());
        }
 
        // Resolve each group in turn (highest priority resolved last)
        debug("Groups for %s: %s", playerName, groups);

        List<String> resolveOrder = new ArrayList<>();
        for (String group : groups) {
            calculateResolutionOrder(resolveOrder, group);
        }
        debug("Resolution order for %s: %s", playerName, resolveOrder);

        List<Entry> entries = new ArrayList<>();
        resolveGroupHelper(entries, groups, resolveOrder);
        
        Map<String, Boolean> permissions;
        if (isInterleavedPlayerPermissions()) {
            // Player-specific permissions overrides group permissions (at same level)
            entries.addAll(getPermissionService().getEntries(playerName, uuid, false));

            permissions = applyPermissions(entries, regions, world);
        }
        else {
            // Apply all player-specific permissions at the end
            permissions = applyPermissions(entries, regions, world);
            
            permissions.putAll(applyPermissions(getPermissionService().getEntries(playerName, uuid, false), regions, world));
        }

        return new ResolverResult(permissions, new LinkedHashSet<>(resolveOrder));
    }

    /**
     * Resolve a group's permissions. The permissions from the group's furthest
     * ancestor are applied first, followed by each succeeding ancestor. (And
     * finally ending with the group itself.)
     * 
     * @param groupName the group's name
     * @param world the destination world name in lowercase or null
     * @param regions the name of the regions containing the destination, all
     *   in lowercase
     * @return effective permissions for this group
     */
    public Map<String, Boolean> resolveGroup(String groupName, String world, Set<String> regions) {
        List<String> resolveOrder = new ArrayList<>();
        calculateResolutionOrder(resolveOrder, groupName);

        List<Entry> entries = new ArrayList<>();
        resolveGroupHelper(entries, Collections.singletonList(groupName), resolveOrder);

        return applyPermissions(entries, regions, world);
    }

    // Determine the order in which groups should be resolved
    private void calculateResolutionOrder(List<String> resolveOrder, String group) {
        List<String> ancestry = getPermissionService().getAncestry(group);
        if (ancestry.isEmpty()) {
            // This only happens when the default group does not exist
            ancestry.add(getDefaultGroup());
        }
        debug("Ancestry for %s: %s", group, ancestry);
        
        for (String ancestor : ancestry) {
            if (isOpaqueInheritance()) {
                // Last appearance wins
                resolveOrder.remove(ancestor);
                resolveOrder.add(ancestor);
            }
            else {
                // First appearance wins
                if (!resolveOrder.contains(ancestor))
                    resolveOrder.add(ancestor);
            }
        }
    }

    // Add ancillary permissions and permissions from each resolved group
    private void resolveGroupHelper(List<Entry> entries, List<String> assignedGroups, List<String> resolveOrder) {
        Set<String> assigned = new HashSet<>(assignedGroups); // for contains()

        for (String group : resolveOrder) {
            if (assigned.contains(group)) {
                if (!getDefaultGroup().equalsIgnoreCase(group) || isIncludeDefaultInAssigned()) {
                    // Add assigned group permissions, if present
                    for (String groupPermissionFormat : getAssignedGroupPermissionFormats()) {
                        Entry groupPerm = new Entry();
                        groupPerm.setPermission(String.format(groupPermissionFormat, group).toLowerCase());
                        groupPerm.setValue(true);
                        entries.add(groupPerm);
                    }
                }
            }

            // Add group permissions, if present
            for (String groupPermissionFormat : getGroupPermissionFormats()) {
                Entry groupPerm = new Entry();
                groupPerm.setPermission(String.format(groupPermissionFormat, group).toLowerCase());
                groupPerm.setValue(true);
                entries.add(groupPerm);
            }

            entries.addAll(getPermissionService().getEntries(group, null, true));
        }
    }

    // Fetches target world, if aliased
    private String getWorldAlias(String world) {
        Object alias = worldAliasCache.get(world); // NB Should only contain Strings or NULL_ALIAS
        if (alias == null) {
            // This world has not yet been seen
            // Go through aliases in order
            outer:
            for (Map.Entry<String, List<Pattern>> me : worldAliases.entrySet()) {
                String target = me.getKey();
                List<Pattern> patterns = me.getValue();
                // Attempt to match patterns
                for (Pattern p : patterns) {
                    Matcher m = p.matcher(world);
                    if (m.matches()) {
                        alias = target;
                        break outer;
                    }
                }
            }
            if (alias == null) {
                // No alias found
                alias = NULL_ALIAS;
            }
            // Set alias cache
            worldAliasCache.put(world, alias);
        }
        return alias != NULL_ALIAS ? (String)alias : null;
    }

    // Apply an entity's permissions to the permission map. Universal permissions
    // (ones not assigned to any specific world) are applied first. They are
    // then overridden by any world-specific permissions.
    private Map<String, Boolean> applyPermissions(List<Entry> entries, Set<String> regions, String world) {
        String worldAlias = world != null ? getWorldAlias(world) : null;

        Map<String, Boolean> permissions = new LinkedHashMap<>();

        Map<String, Map<String, Boolean>> regionPermissions = new HashMap<>();
        List<Entry> worldPermissions = new ArrayList<>();

        // Apply non-region-specific, non-world-specific permissions first
        for (Entry e : entries) {
            if (e.getRegion() == null && e.getWorld() == null) {
                permissions.put(e.getPermission(), e.isValue());
            }
            else if (e.getRegion() != null && e.getWorld() == null) {
                // Universal region-specific (should these really be supported?)
                if (regions.contains(e.getRegion().getName())) {
                    addRegionPermission(regionPermissions, e);
                }
            }
            else if (e.getWorld().getName().equals(world) || e.getWorld().getName().equals(worldAlias)) {
                worldPermissions.add(e);
            }
        }

        // Then override with world-specific permissions
        Map<String, Boolean> specificWorldPermissions = new LinkedHashMap<>();
        Map<String, Map<String, Boolean>> regionWorldAliasPermissions = new HashMap<>();
        Map<String, Map<String, Boolean>> regionWorldPermissions = new HashMap<>();
        for (Entry e : worldPermissions) {
            if (e.getRegion() == null) {
                // Non region-specific
                if (e.getWorld().getName().equals(worldAlias)) {
                    permissions.put(e.getPermission(), e.isValue());
                }
                else {
                    specificWorldPermissions.put(e.getPermission(), e.isValue());
                }
            }
            else {
                if (regions.contains(e.getRegion().getName())) {
                    if (e.getWorld().getName().equals(worldAlias)) {
                        addRegionPermission(regionWorldAliasPermissions, e);
                    }
                    else {
                        addRegionPermission(regionWorldPermissions, e);
                    }
                }
            }
        }
        
        permissions.putAll(specificWorldPermissions);

        // Override with universal, region-specific permissions
        applyRegionPermissions(permissions, regionPermissions, regions);

        // Finally, override with region- and world-specific permissions
        applyRegionPermissions(permissions, regionWorldAliasPermissions, regions);
        applyRegionPermissions(permissions, regionWorldPermissions, regions);
        
        return permissions;
    }

    private void addRegionPermission(Map<String, Map<String, Boolean>> regionPermissions, Entry e) {
        String region = e.getRegion().getName();
        Map<String, Boolean> regionPerms = regionPermissions.get(region);
        if (regionPerms == null) {
            regionPerms = new LinkedHashMap<>();
            regionPermissions.put(region, regionPerms);
        }
        regionPerms.put(e.getPermission(), e.isValue());
    }

    private void applyRegionPermissions(Map<String, Boolean> permissions, Map<String, Map<String, Boolean>> regionPermissions, Set<String> regions) {
        // Depends on iteration order of regions set.
        // If ordering matters, pass a LinkedHashSet to resolvePlayer/resolveGroup
        // NB Ordering must be lowest to highest priority
        for (String region : regions) {
            Map<String, Boolean> regionPerms = regionPermissions.get(region);
            if (regionPerms != null)
                permissions.putAll(regionPerms);
        }
    }

    public MetadataResult resolvePlayerMetadata(UUID uuid) {
        String playerName = UuidUtils.canonicalizeUuid(uuid);
        // Get this player's groups
        List<String> groups = Utils.toGroupNames(Utils.filterExpired(getPermissionService().getGroups(uuid)));
        if (groups.isEmpty()) {
            // If no groups, use the default group
            groups.add(getDefaultGroup());
        }
 
        // Resolve each group in turn (highest priority resolved last)
        List<String> resolveOrder = new ArrayList<>();
        for (String group : groups) {
            calculateResolutionOrder(resolveOrder, group);
        }
        
        List<EntityMetadata> metadata = new ArrayList<>();
        for (String group : resolveOrder) {
            metadata.addAll(getPermissionService().getAllMetadata(group, null, true));
        }
        
        // NB There's only one level, so interleavedPlayerPermissions doesn't matter
        // Always resolve player last
        metadata.addAll(getPermissionService().getAllMetadata(playerName, uuid, false));
        
        return new MetadataResult(applyMetadata(metadata), new LinkedHashSet<>(resolveOrder));
    }

    public MetadataResult resolveGroupMetadata(String groupName) {
        List<String> resolveOrder = new ArrayList<>();
        calculateResolutionOrder(resolveOrder, groupName);

        List<EntityMetadata> metadata = new ArrayList<>();
        for (String group : resolveOrder) {
            metadata.addAll(getPermissionService().getAllMetadata(group, null, true));
        }
        
        return new MetadataResult(applyMetadata(metadata), new LinkedHashSet<>(resolveOrder));
    }

    private Map<String, Object> applyMetadata(List<EntityMetadata> metadata) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Since there are no world-specific or region-specific metadata,
        // simply apply them in order. Last one wins.
        for (EntityMetadata em : metadata) {
            result.put(em.getName(), em.getValue());
        }

        return result;
    }

    public static class ResolverResult {
        
        private final Map<String, Boolean> permissions;
        
        private final Set<String> groups;
        
        private ResolverResult(Map<String, Boolean> permissions, Set<String> groups) {
            this.permissions = permissions;
            this.groups = groups;
        }

        public Map<String, Boolean> getPermissions() {
            return permissions;
        }

        public Set<String> getGroups() {
            return groups;
        }
        
    }

    public static class MetadataResult {
        
        private final Map<String, Object> metadata;
        
        private final Set<String> groups;

        public MetadataResult(Map<String, Object> metadata, Set<String> groups) {
            this.metadata = metadata;
            this.groups = groups;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public Set<String> getGroups() {
            return groups;
        }

    }

}
