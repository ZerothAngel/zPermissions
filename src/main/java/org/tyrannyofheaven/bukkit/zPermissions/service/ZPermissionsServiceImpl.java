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
package org.tyrannyofheaven.bukkit.zPermissions.service;

import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.hasText;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.PermissionsResolver;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionDao;

/**
 * Simple implementation of {@link ZPermissionsService}.
 * 
 * @author zerothangel
 */
public class ZPermissionsServiceImpl implements ZPermissionsService {

    private final PermissionsResolver resolver;

    private final PermissionDao dao;

    private final TransactionStrategy transactionStrategy;

    public ZPermissionsServiceImpl(PermissionsResolver resolver, PermissionDao dao, TransactionStrategy transactionStrategy) {
        if (resolver == null)
            throw new IllegalArgumentException("resolver cannot be null");
        if (dao == null)
            throw new IllegalArgumentException("dao cannot be null");
        if (transactionStrategy == null)
            throw new IllegalArgumentException("transactionStrategy cannot be null");
        this.resolver = resolver;
        this.dao = dao;
        this.transactionStrategy = transactionStrategy;
    }

    private PermissionsResolver getResolver() {
        return resolver;
    }
    
    private PermissionDao getDao() {
        return dao;
    }

    private TransactionStrategy getTransactionStrategy() {
        return transactionStrategy;
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
    @Override
    public List<String> getPlayerAssignedGroups(String playerName) {
        if (!hasText(playerName))
            throw new IllegalArgumentException("playerName must have a value");

        List<String> groups = getDao().getGroups(playerName);
        // Returned groups are in application order, so reverse them
        Collections.reverse(groups);
        // The first group of the returned list is the highest priority
        return groups;
    }

    /* (non-Javadoc)
     * @see org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService#getPlayerGroups(java.lang.String)
     */
    @Override
    public Set<String> getPlayerGroups(final String playerName) {
        if (!hasText(playerName))
            throw new IllegalArgumentException("playerName must have a value");

        final Set<String> result = new HashSet<String>();

        getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                for (String group : getDao().getGroups(playerName)) {
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
        });

        return result;
    }

    /* (non-Javadoc)
     * @see org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService#getGroupPermissions(java.lang.String, java.util.Set, java.lang.String)
     */
    @Override
    public Map<String, Boolean> getGroupPermissions(final String worldName, Set<String> regionNames, final String groupName) {
        if (!hasText(worldName))
            throw new IllegalArgumentException("worldName must have a value");
        if (regionNames == null)
            regionNames = Collections.emptySet();
        if (!hasText(groupName))
            throw new IllegalArgumentException("groupName must have a value");

        // Ensure all region names are lowercased
        final Set<String> regions = new HashSet<String>();
        for (String regionName : regionNames) {
            regions.add(regionName.toLowerCase());
        }

        Map<String, Boolean> permissions = getTransactionStrategy().execute(new TransactionCallback<Map<String, Boolean>>() {
            @Override
            public Map<String, Boolean> doInTransaction() throws Exception {
                return getResolver().resolveGroup(groupName.toLowerCase(), worldName.toLowerCase(), regions);
            }
        });

        return permissions;
    }

    /* (non-Javadoc)
     * @see org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService#getPlayerPermissions(java.lang.String, java.util.Set, java.lang.String)
     */
    @Override
    public Map<String, Boolean> getPlayerPermissions(final String worldName, Set<String> regionNames, final String playerName) {
        if (!hasText(worldName))
            throw new IllegalArgumentException("worldName must have a value");
        if (regionNames == null)
            regionNames = Collections.emptySet();
        if (!hasText(playerName))
            throw new IllegalArgumentException("playerName must have a value");

        // Ensure all region names are lowercased
        final Set<String> regions = new HashSet<String>();
        for (String regionName : regionNames) {
            regions.add(regionName.toLowerCase());
        }

        Map<String, Boolean> permissions = getTransactionStrategy().execute(new TransactionCallback<Map<String, Boolean>>() {
            @Override
            public Map<String, Boolean> doInTransaction() throws Exception {
                return getResolver().resolvePlayer(playerName.toLowerCase(), worldName.toLowerCase(), regions);
            }
        });

        return permissions;
    }

    /* (non-Javadoc)
     * @see org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService#getAllPlayers()
     */
    @Override
    public Set<String> getAllPlayers() {
        Set<String> players = new HashSet<String>();
        
        for (String playerName : getDao().getEntityNames(false)) {
            players.add(playerName);
        }

        return players;
    }

    /* (non-Javadoc)
     * @see org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService#getGroupMembers(java.lang.String)
     */
    @Override
    public Set<String> getGroupMembers(String groupName) {
        if (!hasText(groupName))
            throw new IllegalArgumentException("groupName must have a value");
        // DAO returns them in alphabetical order. This interface doesn't care
        // about ordering.
        return new HashSet<String>(getDao().getMembers(groupName));
    }

}
