/*
 * Copyright 2014 ZerothAngel <zerothangel@tyrannyofheaven.org>
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
package org.tyrannyofheaven.bukkit.zPermissions.util;

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.log;
import static org.tyrannyofheaven.bukkit.zPermissions.util.Utils.formatPlayerName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.zPermissions.PermissionsResolver;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.storage.StorageStrategy;

public class SearchTask implements Runnable {

    private static final AtomicInteger searchIdGenerator = new AtomicInteger();

    private final int searchId;

    private final Plugin plugin;

    private final StorageStrategy storageStrategy;

    private final PermissionsResolver resolver;

    private final String permission;

    private final List<UUID> players;
    
    private final List<String> groups;
    
    private final boolean effective;
    
    private final String world;
    
    private final Set<String> regions;

    private final boolean showUuid;

    private int batchSize = 1;
    
    private int delay = 5;

    public SearchTask(Plugin plugin, StorageStrategy storageStrategy, PermissionsResolver resolver, String permission, List<UUID> players, List<String> groups, boolean effective, String world, Set<String> regions, boolean showUuid) {
        this.searchId = searchIdGenerator.incrementAndGet();
        this.plugin = plugin;
        this.storageStrategy = storageStrategy;
        this.resolver = resolver;
        this.permission = permission.toLowerCase();
        this.players = players;
        this.groups = groups;
        this.effective = effective;
        this.world = world;
        this.regions = regions;
        this.showUuid = showUuid;
    }

    public int getSearchId() {
        return searchId;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    @Override
    public void run() {
        int size = 0;
        
        while (size < getBatchSize() && !players.isEmpty()) {
            UUID uuid = players.remove(0);
            
            PermissionEntity entity = storageStrategy.getPermissionService().getEntity("ignored", uuid, false);
            if (entity != null && !entity.getPermissions().isEmpty()) {
                if (checkPermissions(entity) || (effective && checkEffectivePermissions(entity))) {
                    log(plugin, "Search result (#%d): player %s", getSearchId(), formatPlayerName(entity, showUuid));
                }
            }
            
            size++;
        }

        while (size < getBatchSize() && !groups.isEmpty()) {
            String groupName = groups.remove(0);
            
            PermissionEntity entity = storageStrategy.getPermissionService().getEntity(groupName, null, true);
            if (entity != null && !entity.getPermissions().isEmpty()) {
                if (checkPermissions(entity) || (effective && checkEffectivePermissions(entity))) {
                    log(plugin, "Search result (#%d): group %s", getSearchId(), entity.getDisplayName());
                }
            }
            
            size++;
        }
        
        if (players.isEmpty() && groups.isEmpty()) {
            log(plugin, "Search result (#%d): All done!", getSearchId());
        }
        else {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this, getDelay());
        }
    }

    private boolean checkPermissions(PermissionEntity entity) {
        for (Entry e : entity.getPermissions()) {
            if (e.getPermission().contains(permission))
                return true;
        }
        return false;
    }

    private boolean checkEffectivePermissions(final PermissionEntity entity) {
        Map<String, Boolean> rootPermissions = storageStrategy.getTransactionStrategy().execute(new TransactionCallback<Map<String, Boolean>>() {
            @Override
            public Map<String, Boolean> doInTransaction() throws Exception {
                if (entity.isGroup()) {
                    return resolver.resolveGroup(entity.getDisplayName(), world, regions);
                }
                else {
                    return resolver.resolvePlayer(entity.getUuid(), world, regions).getPermissions();
                }
            }
        }, true);
        Map<String, Boolean> permissions = new HashMap<>();
        Utils.calculateChildPermissions(permissions, rootPermissions, false);
        for (String k : permissions.keySet()) {
            if (k.contains(permission))
                return true;
        }
        return false;
    }

}
