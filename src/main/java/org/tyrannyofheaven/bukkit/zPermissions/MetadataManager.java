/*
 * Copyright 2013 ZerothAngel <zerothangel@tyrannyofheaven.org>
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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.PermissionsResolver.MetadataResult;

/**
 * Manager for resolved metadata.
 * 
 * @author zerothangel
 */
public class MetadataManager {

    private static final int PLAYER_CACHE_SIZE = 1000;
    
    private static final int GROUP_CACHE_SIZE = 1000;

    private final PermissionsResolver resolver;

    private final TransactionStrategy transactionStrategy;

    // Simple brain-dead caches instead of Guava Caches because manual management
    // in Guava <11 is hard without a CacheLoader.
    private final Map<UUID, CacheEntry> playerCache = new LinkedHashMap<UUID, CacheEntry>() {
        private static final long serialVersionUID = -3392138700819296598L;

        @Override
        protected boolean removeEldestEntry(Entry<UUID, CacheEntry> eldest) {
            return size() > PLAYER_CACHE_SIZE;
        }
    };
    
    private final Map<String, CacheEntry> groupCache = new LinkedHashMap<String, CacheEntry>() {
        private static final long serialVersionUID = 535803145911477635L;

        @Override
        protected boolean removeEldestEntry(Entry<String, CacheEntry> eldest) {
            return size() > GROUP_CACHE_SIZE;
        }
    };

    // Lock for both of the above. Even though we're supposed to be
    // single-threaded, many MANY chat plugins are inherently broken and
    // call Bukkit/Vault methods outside the main thread.
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();

    public MetadataManager(PermissionsResolver resolver, TransactionStrategy transactionStrategy) {
        this.resolver = resolver;
        this.transactionStrategy = transactionStrategy;
        
    }

    private PermissionsResolver getResolver() {
        return resolver;
    }

    private TransactionStrategy getTransactionStrategy() {
        return transactionStrategy;
    }

    public Object getMetadata(String name, final UUID uuid, final boolean group, String metadataName) {
        if (!group && uuid == null)
            throw new IllegalArgumentException("uuid cannot be null");

        final String lname = name.toLowerCase();

        Map<String, Object> metadata;

        cacheLock.readLock().lock();
        try {
            metadata = getCachedMetadata(lname, uuid, group);

            if (metadata == null) {
                cacheLock.readLock().unlock(); // Can't upgrade
                cacheLock.writeLock().lock();
                try {
                    // Re-test
                    metadata = getCachedMetadata(lname, uuid, group);

                    if (metadata == null) {
                        // Not in cache, look it up
                        MetadataResult metadataResult = getTransactionStrategy().execute(new TransactionCallback<MetadataResult>() {
                            @Override
                            public MetadataResult doInTransaction() throws Exception {
                                if (group)
                                    return getResolver().resolveGroupMetadata(lname);
                                else
                                    return getResolver().resolvePlayerMetadata(uuid);
                            }
                        });
                        CacheEntry pe = new CacheEntry(metadataResult.getMetadata(), metadataResult.getGroups());

                        if (group) {
                            // Stuff in cache
                            groupCache.put(lname, pe);
                        }
                        else {
                            // Stuff in cache
                            playerCache.put(uuid, pe);
                        }
                        metadata = metadataResult.getMetadata();
                    }
                }
                finally {
                    cacheLock.readLock().lock(); // Downgrade lock
                    cacheLock.writeLock().unlock();
                }
            }

            return metadata.get(metadataName.toLowerCase());
        }
        finally {
            cacheLock.readLock().unlock();
        }
    }

    private Map<String, Object> getCachedMetadata(final String lname, UUID uuid, boolean group) {
        // Check cache first
        CacheEntry pe;
        if (group) {
            pe = groupCache.get(lname);
        }
        else {
            pe = playerCache.get(uuid);
        }
        return pe != null ? pe.getMetadata() : null;
    }

    public void invalidateMetadata(String name, UUID uuid, boolean group) {
        if (!group && uuid == null)
            throw new IllegalArgumentException("uuid cannot be null");

        name = name.toLowerCase();

        cacheLock.writeLock().lock();
        try {
            if (group) {
                groupCache.remove(name);
                // Also invalidate related players
                for (Iterator<Map.Entry<UUID, CacheEntry>> i = playerCache.entrySet().iterator(); i.hasNext();) {
                    Map.Entry<UUID, CacheEntry> me = i.next();
                    if (me.getValue().getGroups().contains(name)) {
                        i.remove();
                    }
                }
                // And related groups
                for (Iterator<Map.Entry<String, CacheEntry>> i = groupCache.entrySet().iterator(); i.hasNext();) {
                    Map.Entry<String, CacheEntry> me = i.next();
                    if (me.getValue().getGroups().contains(name)) {
                        i.remove();
                    }
                }
            }
            else {
                playerCache.remove(uuid);
            }
        }
        finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    public void invalidateAllMetadata() {
        cacheLock.writeLock().lock();
        try {
            playerCache.clear();
            groupCache.clear();
        }
        finally {
            cacheLock.writeLock().unlock();
        }
    }

    private static class CacheEntry {
        
        private final Map<String, Object> metadata;
        
        private final Set<String> groups;

        public CacheEntry(Map<String, Object> metadata, Set<String> groups) {
            this.metadata = Collections.unmodifiableMap(metadata);

            // Lowercase groups
            Set<String> groupsCopy = new LinkedHashSet<>(groups.size());
            for (String group : groups) {
                groupsCopy.add(group.toLowerCase());
            }
            this.groups = Collections.unmodifiableSet(groupsCopy);
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public Set<String> getGroups() {
            return groups;
        }
        
    }

}
