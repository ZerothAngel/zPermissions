/*
 * Copyright 2012 ZerothAngel <zerothangel@tyrannyofheaven.org>
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Programmatic interface of read-only operations for things that are not easily
 * done solely by command line (i.e. would involve parsing command output, which
 * would be messy).
 */
public interface ZPermissionsService {

    /**
     * Retrieve names of all players with permissions defined.
     * 
     * @return set of players known by zPermissions, not including those who are
     *   only group members
     */
    @Deprecated
    public Set<String> getAllPlayers();

    /**
     * Retrieve UUIDs of all players with permissions defined.
     * 
     * @return set of players known by zPermissions, not including those who are
     *   only group members
     */
    public Set<UUID> getAllPlayersUUID();

    /**
     * Resolve a player's permissions for the given world and region set. The
     * returned map is ultimately what zPermissions uses to create the player's
     * Bukkit attachment.
     * 
     * <p>Note that Bukkit does some additional magic behind the scenes, specifically
     * regarding default permission values (which are determined by the
     * e.g. plugin.yml of the plugin that owns the permission).
     * 
     * <p>In other words, just looking at the returned effective permissions will
     * <strong>not</strong> replicate the behavior of hasPermissions()! You
     * must take defaults into account. I leave that to the caller...
     * 
     * @param worldName the name of the target world. May be <code>null</code>.
     * @param regionNames set of region names. May be <code>null</code>.
     * @param uuid the player's UUID
     * @return effective permissions for this player
     */
    @Deprecated
    public Map<String, Boolean> getPlayerPermissions(String worldName, Set<String> regionNames, String playerName);
    
    /**
     * Resolve a player's permissions for the given world and region set. The
     * returned map is ultimately what zPermissions uses to create the player's
     * Bukkit attachment.
     * 
     * <p>Note that Bukkit does some additional magic behind the scenes, specifically
     * regarding default permission values (which are determined by the
     * e.g. plugin.yml of the plugin that owns the permission).
     * 
     * <p>In other words, just looking at the returned effective permissions will
     * <strong>not</strong> replicate the behavior of hasPermissions()! You
     * must take defaults into account. I leave that to the caller...
     * 
     * @param worldName the name of the target world. May be <code>null</code>.
     * @param regionNames set of region names. May be <code>null</code>.
     * @param playerName the player's name
     * @return effective permissions for this player
     */
    public Map<String, Boolean> getPlayerPermissions(String worldName, Set<String> regionNames, UUID uuid);

    /**
     * Retrieve groups which a player is explicitly assigned. The groups are
     * returned in priority order, with the highest priority first. (This can
     * possibly be considered the player's "primary" group.) If the player has
     * no explicitly assigned groups, the default group is returned.
     * 
     * @param playerName the player's name
     * @return the names of groups which the player is assigned to
     */
    @Deprecated
    public List<String> getPlayerAssignedGroups(String playerName);

    /**
     * Retrieve groups which a player is explicitly assigned. The groups are
     * returned in priority order, with the highest priority first. (This can
     * possibly be considered the player's "primary" group.) If the player has
     * no explicitly assigned groups, the default group is returned.
     * 
     * @param uuid the player's UUID
     * @return the names of groups which the player is assigned to
     */
    public List<String> getPlayerAssignedGroups(UUID uuid);

    /**
     * Retrieve groups which a player is a member of. This includes all
     * assigned groups as well as their ancestor groups.
     * 
     * @param playerName the player's name
     * @return the names of groups which the player is a member of
     */
    @Deprecated
    public Set<String> getPlayerGroups(String playerName);

    /**
     * Retrieve groups which a player is a member of. This includes all
     * assigned groups as well as their ancestor groups.
     * 
     * @param uuid the player's UUID
     * @return the names of groups which the player is a member of
     */
    public Set<String> getPlayerGroups(UUID uuid);

    /**
     * Retrieve names of all groups.
     * 
     * @return names of all groups known by zPermissions
     */
    public Set<String> getAllGroups();

    /**
     * Resolve a group's permissions for the given world and region set.
     * 
     * <p>The returned map contains permissions which are explicitly defined by
     * the group or its ancestors.
     * 
     * <p>You <strong>must</strong> properly handle defaults do something like
     * Bukkit's hasPermissions() method. See {@link #getPlayerPermissions(String, Set, String)}
     * for further explanation.
     * 
     * @param worldName the name of the target world. May be <code>null</code>.
     * @param regionNames set of region names. May be <code>null</code>.
     * @param groupName the group's name
     * @return effective permissions for this group
     */
    public Map<String, Boolean> getGroupPermissions(String worldName, Set<String> regionNames, String groupName);

    /**
     * Retrieve the names of the players that are members of the given group.
     * 
     * @param groupName the group's name
     * @return the group's members
     */
    public Set<String> getGroupMembers(String groupName);

    /**
     * Retrieve the UUIDs of the players that are members of the given group.
     * 
     * @param groupName the group's name
     * @return the group's members
     */
    public Set<UUID> getGroupMembersUUID(String groupName);

    /**
     * Retrieve the named metadata value from a player.
     * 
     * @param playerName the player's name
     * @param metadataName the name of the metadata value
     * @param type the metadata type (String, Integer, Long, Float, Double, Boolean, Object).
     *     Integers and Floats may be truncated due to the source value having more precision.
     * @return the metadata value or null if not found
     * @throws IllegalStateException if the actual metadata type does not match the given type
     */
    @Deprecated
    public <T> T getPlayerMetadata(String playerName, String metadataName, Class<T> type);
    
    /**
     * Retrieve the named metadata value from a player.
     * 
     * @param uuid the player's UUID
     * @param metadataName the name of the metadata value
     * @param type the metadata type (String, Integer, Long, Float, Double, Boolean, Object).
     *     Integers and Floats may be truncated due to the source value having more precision.
     * @return the metadata value or null if not found
     * @throws IllegalStateException if the actual metadata type does not match the given type
     */
    public <T> T getPlayerMetadata(UUID uuid, String metadataName, Class<T> type);

    /**
     * Retrieve the named metadata value from a group.
     * 
     * @param playerName the group's name
     * @param metadataName the name of the metadata value
     * @param type the metadata type (String, Integer, Long, Float, Double, Boolean, Object)
     *     Integers and Floats may be truncated due to the source value having more precision.
     * @return the metadata value or null if not found
     * @throws IllegalStateException if the actual metadata type does not match the given type
     */
    public <T> T getGroupMetadata(String groupName, String metadataName, Class<T> type);

    /**
     * Retrieve names of all defined tracks.
     * 
     * @return names of all tracks
     */
    public Set<String> getAllTracks();

    /**
     * Retrieve group names of the given track in rank order.
     * 
     * @param trackName the track name
     * @return the group names of the track
     */
    public List<String> getTrackGroups(String trackName);

    /**
     * zPermissions does not have any concept of "primary group." The closest
     * thing to this would be the highest-weight assigned group. However, for
     * flexibility, there are other ways of determining the primary group, usually
     * involving metadata of some kind. This method is an attempt to internalize
     * all that logic within zPermissions (it previously existed solely in Vault).
     * 
     * @param playerName the name of the player
     * @return the name of the player's primary group
     */
    @Deprecated
    public String getPlayerPrimaryGroup(String playerName);

    /**
     * zPermissions does not have any concept of "primary group." The closest
     * thing to this would be the highest-weight assigned group. However, for
     * flexibility, there are other ways of determining the primary group, usually
     * involving metadata of some kind. This method is an attempt to internalize
     * all that logic within zPermissions (it previously existed solely in Vault).
     * 
     * @param uuid the UUID of the player
     * @return the name of the player's primary group
     */
    public String getPlayerPrimaryGroup(UUID uuid);

    /**
     * Retrieve a player's chat prefix.
     * 
     * @param uuid the player's UUID
     * @return the player's prefix or empty string if none
     */
    public String getPlayerPrefix(UUID uuid);

    /**
     * Retrieve a player's suffix.
     * 
     * @param uuid the player's UUID
     * @return the player's suffix or empty string if none
     */
    public String getPlayerSuffix(UUID uuid);

}
