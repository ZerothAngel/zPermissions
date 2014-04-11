/*
 * Copyright 2012 Allan Saddi <allan@saddi.com>
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
package org.tyrannyofheaven.bukkit.zPermissions.service;

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.warn;
import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.hasText;
import static org.tyrannyofheaven.bukkit.zPermissions.uuid.UuidUtils.canonicalizeUuid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.MetadataManager;
import org.tyrannyofheaven.bukkit.zPermissions.PermissionsResolver;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsConfig;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.util.MetadataConstants;
import org.tyrannyofheaven.bukkit.zPermissions.util.Utils;

/**
 * Simple implementation of {@link ZPermissionsService}.
 * 
 * @author asaddi
 */
public class ZPermissionsServiceImpl implements ZPermissionsService {

    private static final Set<Class<?>> validMetadataTypes;

    private final Plugin plugin;

    private final PermissionsResolver resolver;

    private final PermissionDao dao;

    private final MetadataManager metadataManager;

    private final TransactionStrategy transactionStrategy;

    private final ZPermissionsConfig config;

    static {
        Set<Class<?>> types = new HashSet<Class<?>>();
        types.add(Object.class);
        types.add(String.class);
        types.add(Integer.class);
        types.add(Long.class);
        types.add(Float.class);
        types.add(Double.class);
        types.add(Boolean.class);
        validMetadataTypes = Collections.unmodifiableSet(types);
    }

    public ZPermissionsServiceImpl(Plugin plugin, PermissionsResolver resolver, PermissionDao dao, MetadataManager metadataManager, TransactionStrategy transactionStrategy, ZPermissionsConfig config) {
        if (plugin == null)
            throw new IllegalArgumentException("plugin cannot be null");
        if (resolver == null)
            throw new IllegalArgumentException("resolver cannot be null");
        if (dao == null)
            throw new IllegalArgumentException("dao cannot be null");
        if (metadataManager == null)
            throw new IllegalArgumentException("metadataManager cannot be null");
        if (transactionStrategy == null)
            throw new IllegalArgumentException("transactionStrategy cannot be null");
        if (config == null)
            throw new IllegalArgumentException("config cannot be null");
        this.plugin = plugin;
        this.resolver = resolver;
        this.dao = dao;
        this.metadataManager = metadataManager;
        this.transactionStrategy = transactionStrategy;
        this.config = config;
    }

    private PermissionsResolver getResolver() {
        return resolver;
    }
    
    private PermissionDao getDao() {
        return dao;
    }

    private MetadataManager getMetadataManager() {
        return metadataManager;
    }

    private TransactionStrategy getTransactionStrategy() {
        return transactionStrategy;
    }

    private ZPermissionsConfig getZPermissionsConfig() {
        return config;
    }

    /* (non-Javadoc)
     * @see org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService#getAllGroups()
     */
    @Override
    public Set<String> getAllGroups() {
        Set<String> groups = new HashSet<String>();
        
        for (String groupName : getDao().getEntityNames(true)) {
            groups.add(groupName);
        }

        return groups;
    }

    /* (non-Javadoc)
     * @see org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService#getPlayerAssignedGroups(java.lang.String)
     */
    @Deprecated
    @Override
    public List<String> getPlayerAssignedGroups(String playerName) {
        if (!hasText(playerName))
            throw new IllegalArgumentException("playerName must have a value");

        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        if (player == null) return Collections.emptyList();
        UUID uuid = player.getUniqueId();

        return getPlayerAssignedGroups(uuid);
    }

    @Override
    public List<String> getPlayerAssignedGroups(UUID uuid) {
        if (uuid == null)
            throw new IllegalArgumentException("uuid cannot be null");

        List<String> groups = Utils.toGroupNames(Utils.filterExpired(getDao().getGroups(uuid)));
        // NB: Only works because we know returned list is mutable.

        // If totally empty, then they are in the default group.
        if (groups.isEmpty())
            groups.add(getResolver().getDefaultGroup());

        // Returned groups are in application order, so reverse them
        Collections.reverse(groups);
        // The first group of the returned list is the highest priority
        return groups;
    }

    /* (non-Javadoc)
     * @see org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService#getPlayerGroups(java.lang.String)
     */
    @Deprecated
    @Override
    public Set<String> getPlayerGroups(final String playerName) {
        if (!hasText(playerName))
            throw new IllegalArgumentException("playerName must have a value");

        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        if (player == null) return Collections.emptySet();
        UUID uuid = player.getUniqueId();

        return getPlayerGroups(uuid);
    }

    @Override
    public Set<String> getPlayerGroups(final UUID uuid) {
        if (uuid == null)
            throw new IllegalArgumentException("uuid cannot be null");

        final Set<String> result = new LinkedHashSet<String>();

        getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                for (String group : Utils.toGroupNames(Utils.filterExpired(getDao().getGroups(uuid)))) {
                    // Get ancestors
                    List<String> ancestors = getDao().getAncestry(group);
                    if (ancestors.isEmpty()) {
                        // Non-existant default group
                        ancestors.add(getResolver().getDefaultGroup());
                    }

                    // NB: ancestors will include group as well
                    result.addAll(ancestors);
                }
            }
        }, true);

        // If totally empty, then they are in the default group.
        if (result.isEmpty())
            result.add(getResolver().getDefaultGroup());

        return result;
    }

    /* (non-Javadoc)
     * @see org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService#getGroupPermissions(java.lang.String, java.util.Set, java.lang.String)
     */
    @Override
    public Map<String, Boolean> getGroupPermissions(String worldName, Set<String> regionNames, final String groupName) {
        final String lworldName = hasText(worldName) ? worldName.toLowerCase() : null;
        if (regionNames == null)
            regionNames = Collections.emptySet();
        if (!hasText(groupName))
            throw new IllegalArgumentException("groupName must have a value");

        // Ensure all region names are lowercased
        final Set<String> regions = new LinkedHashSet<String>();
        for (String regionName : regionNames) {
            regions.add(regionName.toLowerCase());
        }

        Map<String, Boolean> permissions = getTransactionStrategy().execute(new TransactionCallback<Map<String, Boolean>>() {
            @Override
            public Map<String, Boolean> doInTransaction() throws Exception {
                return getResolver().resolveGroup(groupName.toLowerCase(), lworldName, regions);
            }
        }, true);

        return permissions;
    }

    /* (non-Javadoc)
     * @see org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService#getPlayerPermissions(java.lang.String, java.util.Set, java.lang.String)
     */
    @Deprecated
    @Override
    public Map<String, Boolean> getPlayerPermissions(String worldName, Set<String> regionNames, final String playerName) {
        if (!hasText(playerName))
            throw new IllegalArgumentException("playerName must have a value");

        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        if (player == null) return Collections.emptyMap();
        UUID uuid = player.getUniqueId();
        
        return getPlayerPermissions(worldName, regionNames, uuid);
    }

    @Override
    public Map<String, Boolean> getPlayerPermissions(String worldName, Set<String> regionNames, final UUID uuid) {
        if (uuid == null)
            throw new IllegalArgumentException("uuid cannot be null");

        final String lworldName = hasText(worldName) ? worldName.toLowerCase() : null;
        if (regionNames == null)
            regionNames = Collections.emptySet();

        // Ensure all region names are lowercased
        final Set<String> regions = new LinkedHashSet<String>();
        for (String regionName : regionNames) {
            regions.add(regionName.toLowerCase());
        }

        Map<String, Boolean> permissions = getTransactionStrategy().execute(new TransactionCallback<Map<String, Boolean>>() {
            @Override
            public Map<String, Boolean> doInTransaction() throws Exception {
                return getResolver().resolvePlayer(uuid, lworldName, regions).getPermissions();
            }
        }, true);

        return permissions;
    }

    /* (non-Javadoc)
     * @see org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService#getAllPlayers()
     */
    @Deprecated
    @Override
    public Set<String> getAllPlayers() {
        Set<String> players = new HashSet<String>();
        
        for (String playerName : getDao().getEntityNames(false)) {
            players.add(playerName);
        }

        return players;
    }

    @Override
    public Set<UUID> getAllPlayersUUID() {
        Set<UUID> players = new HashSet<UUID>();
        
        for (PermissionEntity player : getDao().getEntities(false)) {
            players.add(player.getUuid());
        }

        return players;
    }

    /* (non-Javadoc)
     * @see org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService#getGroupMembers(java.lang.String)
     */
    @Deprecated
    @Override
    public Set<String> getGroupMembers(String groupName) {
        if (!hasText(groupName))
            throw new IllegalArgumentException("groupName must have a value");
        // DAO returns them in alphabetical order. This interface doesn't care
        // about ordering.
        return new HashSet<String>(Utils.toMembers(Utils.filterExpired(getDao().getMembers(groupName)), false));
    }

    @Override
    public Set<UUID> getGroupMembersUUID(String groupName) {
        if (!hasText(groupName))
            throw new IllegalArgumentException("groupName must have a value");
        // DAO returns them in alphabetical order. This interface doesn't care
        // about ordering.
        
        Set<UUID> members = new HashSet<UUID>();

        for (Membership membership : Utils.filterExpired(getDao().getMembers(groupName))) {
            members.add(membership.getUuid());
        }

        return members;
    }

    /* (non-Javadoc)
     * @see org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService#getPlayerMetadata(java.lang.String, java.lang.String, java.lang.Class)
     */
    @Deprecated
    @Override
    public <T> T getPlayerMetadata(String playerName, String metadataName, Class<T> type) {
        if (!hasText(playerName))
            throw new IllegalArgumentException("playerName must have a value");

        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        if (player == null) return null;
        UUID uuid = player.getUniqueId();

        return getEntityMetadata(playerName, uuid, false, metadataName, type);
    }

    @Override
    public <T> T getPlayerMetadata(UUID uuid, String metadataName, Class<T> type) {
        if (uuid == null)
            throw new IllegalArgumentException("uuid cannot be null");

        return getEntityMetadata("ignored", uuid, false, metadataName, type);
    }

    /* (non-Javadoc)
     * @see org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService#getGroupMetadata(java.lang.String, java.lang.String, java.lang.Class)
     */
    @Override
    public <T> T getGroupMetadata(String groupName, String metadataName, Class<T> type) {
        if (!hasText(groupName))
            throw new IllegalArgumentException("groupName must have a value");
        return getEntityMetadata(groupName, null, true, metadataName, type);
    }

    private <T> T getEntityMetadata(final String name, final UUID uuid, final boolean group, final String metadataName, Class<T> type) {
        if (!group && uuid == null)
            throw new IllegalArgumentException("uuid cannot be null");
        if (!hasText(metadataName))
            throw new IllegalArgumentException("metadataName must have a value");
        if (type == null)
            throw new IllegalArgumentException("type cannot be null");
        if (!validMetadataTypes.contains(type))
            throw new IllegalArgumentException("Unsupported metadata type");
        
        Object value;
        if (config.isInheritedMetadata()) {
            // Use metadata manager to resolve metadata
            value = getMetadataManager().getMetadata(name, uuid, group, metadataName);
        }
        else {
            value = getTransactionStrategy().execute(new TransactionCallback<Object>() {
                @Override
                public Object doInTransaction() throws Exception {
                    return getDao().getMetadata(name, uuid, group, metadataName);
                }
            }, true);
        }

        if (value == null)
            return null;

        if (type == Object.class)
            return type.cast(value); // Can be any type
        else if (value.getClass() == type)
            return type.cast(value); // Same type, good to go
        else if (type == Integer.class && value.getClass() == Long.class)
            return type.cast(((Number)value).intValue()); // Convert
        else if (type == Float.class && value.getClass() == Double.class)
            return type.cast(((Number)value).floatValue()); // Convert

        throw new IllegalStateException("Mismatched metadata type: " + value.getClass().getSimpleName() + "; expecting: " + type.getSimpleName());
    }

    /* (non-Javadoc)
     * @see org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService#getAllTracks()
     */
    @Override
    public Set<String> getAllTracks() {
        return new HashSet<String>(getZPermissionsConfig().getTracks());
    }

    /* (non-Javadoc)
     * @see org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService#getTrackGroups(java.lang.String)
     */
    @Override
    public List<String> getTrackGroups(String trackName) {
        if (!hasText(trackName))
            throw new IllegalArgumentException("trackName must have a value");
        
        List<String> result = getZPermissionsConfig().getTrack(trackName);
        if (result == null || result.isEmpty())
            throw new IllegalStateException("Track has not been defined");
        // NB make a copy
        return new ArrayList<String>(result);
    }

    /* (non-Javadoc)
     * @see org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService#getPlayerPrimaryGroup(java.lang.String)
     */
    @Deprecated
    @Override
    public String getPlayerPrimaryGroup(String playerName) {
        if (!hasText(playerName))
            throw new IllegalArgumentException("playerName must have a value");

        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        if (player == null) return null;
        UUID uuid = player.getUniqueId();

        return getPlayerPrimaryGroup(uuid);
    }

    @Override
    public String getPlayerPrimaryGroup(UUID uuid) {
        try {
            String track = getPlayerMetadata(uuid, MetadataConstants.PRIMARY_GROUP_TRACK_KEY, String.class);
            if (!hasText(track))
                track = config.getDefaultPrimaryGroupTrack();
            if (hasText(track)) {
                List<String> groups = getTrackGroups(track);
                Collections.reverse(groups); // groups is now high rank to low

                Set<String> trackGroups = new LinkedHashSet<String>(groups);
                trackGroups.retainAll(getPlayerAssignedGroups(uuid)); // intersection with all assigned groups

                if (!trackGroups.isEmpty())
                    return trackGroups.iterator().next(); // return highest-ranked group in given track
            }
        }
        catch (IllegalStateException e) {
            warn(plugin, "Bad property '%s' for %s; is it a string and does the track exist?", MetadataConstants.PRIMARY_GROUP_TRACK_KEY, canonicalizeUuid(uuid));
        }

        // Has no concept of primary group... use highest-priority assigned group instead
        List<String> groups = getPlayerAssignedGroups(uuid);
        if (!groups.isEmpty()) {
            return groups.get(0);
        } else {
            // Shouldn't get here, but just in case
            return resolver.getDefaultGroup();
        }
    }

}
