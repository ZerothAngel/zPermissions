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
package org.tyrannyofheaven.bukkit.zPermissions.storage;

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.debug;
import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.log;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.util.transaction.AsyncTransactionStrategy;
import org.tyrannyofheaven.bukkit.util.transaction.PreBeginHook;
import org.tyrannyofheaven.bukkit.util.transaction.PreCommitHook;
import org.tyrannyofheaven.bukkit.util.transaction.RetryingAvajeTransactionStrategy;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionStrategy;
import org.tyrannyofheaven.bukkit.util.uuid.UuidDisplayName;
import org.tyrannyofheaven.bukkit.util.uuid.UuidResolver;
import org.tyrannyofheaven.bukkit.zPermissions.ReadOnlyException;
import org.tyrannyofheaven.bukkit.zPermissions.dao.AvajePermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.dao.InMemoryPermissionService;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionService;
import org.tyrannyofheaven.bukkit.zPermissions.model.DataVersion;
import org.tyrannyofheaven.bukkit.zPermissions.model.UuidDisplayNameCache;

import com.avaje.ebean.EbeanServer;

/**
 * StorageStrategy for AvajePermissionService.
 * 
 * @author zerothangel
 */
public class AvajeStorageStrategy implements StorageStrategy, PreBeginHook, PreCommitHook, UuidResolver {

    private final InMemoryPermissionService permissionService = new InMemoryPermissionService();

    private final AvajePermissionDao permissionDao;

    private final AsyncTransactionStrategy transactionStrategy;

    private final TransactionStrategy internalTransactionStrategy; // NB private and only used here

    private final Plugin plugin;

    private final ExecutorService executorService;

    private final AtomicLong lastLoadedVersion = new AtomicLong(0L);

    private final boolean readOnlyMode;

    private long uuidCacheTimeout = 120L * 60L * 1000L; // Default to 2 hours

    public AvajeStorageStrategy(Plugin plugin, int maxRetries, boolean readOnlyMode) {
        // Following will be used to actually execute async
        executorService = Executors.newSingleThreadExecutor();

        transactionStrategy = new AsyncTransactionStrategy(new RetryingAvajeTransactionStrategy(plugin.getDatabase(), maxRetries, null, this), executorService, this);
        permissionDao = new AvajePermissionDao(permissionService, plugin.getDatabase(), transactionStrategy.getExecutor());
        permissionService.setPermissionDao(permissionDao);
        // NB internalTransactionStrategy has no pre-commit hook since it falls
        // outside the purview of data versioning. data versioning = permissions system only.
        // All reads are uncached. Writes only occur to UUID cache.
        internalTransactionStrategy = new RetryingAvajeTransactionStrategy(plugin.getDatabase(), maxRetries);
        this.plugin = plugin;
        this.readOnlyMode = readOnlyMode;
    }

    @Override
    public void init(Map<String, Object> configMap) {
        // Take care of storage-specific configuration first
        Number uuidCacheTimeout = (Number)configMap.get("uuid-database-cache-ttl");
        if (uuidCacheTimeout != null) {
            this.uuidCacheTimeout = uuidCacheTimeout.longValue() * 60L * 1000L;
            debug(plugin, "AvajeStorageStrategy uuidCacheTimeout = %d", this.uuidCacheTimeout);
        }

        log(plugin, "Loading all permissions from database...");
//        plugin.getDatabase().getAdminLogging().setDebugGeneratedSql(true);
        long start = System.currentTimeMillis();
        refreshInternal(true); // synchronously
        log(plugin, "Finished initial load (%d ms).", System.currentTimeMillis() - start);
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
        try {
            long timeout = 60L;
            log(plugin, "Waiting up to %d seconds for pending write operations...", timeout);
            if (!executorService.awaitTermination(timeout, TimeUnit.SECONDS)) {
                log(plugin, Level.WARNING, "Timed out before all write operations could finish; expect inconsistencies :(");
            }
            else {
                log(plugin, "All write operations done.");
            }
        }
        catch (InterruptedException e) {
            // Do nothing
        }
    }

    @Override
    public void refresh(final boolean force, final Runnable finishTask) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                if (refreshInternal(force) && finishTask != null)
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, finishTask);
            }
        });
    }

    private boolean refreshInternal(final boolean force) {
        return internalTransactionStrategy.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                DataVersion currentVersion = getCurrentDataVersion();

                if (force || lastLoadedVersion.get() != currentVersion.getVersion()) {
                    permissionDao.load();
                    lastLoadedVersion.set(currentVersion.getVersion());
                    return true;
                }
                
                return false;
            }
        }, true);
    }

    @Override
    public PermissionService getPermissionService() {
        return permissionService;
    }

    @Override
    public TransactionStrategy getTransactionStrategy() {
        return transactionStrategy;
    }

    @Override
    public TransactionStrategy getRetryingTransactionStrategy() {
        return transactionStrategy;
    }

    private EbeanServer getEbeanServer() {
        return plugin.getDatabase();
    }

    // Return current data version. Will never return null.
    private DataVersion getCurrentDataVersion() {
        DataVersion dv = getEbeanServer().find(DataVersion.class).where()
                .eq("name", plugin.getName())
                .findUnique();
        if (dv == null) {
            // Make one up
            dv = new DataVersion();
            dv.setName(plugin.getName());
            dv.setVersion(0L);
            dv.setTimestamp(new Date());
        }
        
        return dv;
    }

    @Override
    public void preCommit(boolean readOnly) throws Exception {
        if (readOnly) return; // Do nothing for read-only transactions
        
        DataVersion dv = getCurrentDataVersion();

        // Only update our internal data version if it matches the current version
        long previousVersion = dv.getVersion();

        // Increment version and update timestamp
        dv.setVersion(dv.getVersion() + 1L);
        dv.setTimestamp(new Date());
        // Save
        getEbeanServer().save(dv);

        lastLoadedVersion.compareAndSet(previousVersion, dv.getVersion()); // probably not the appropriate place. Should really be post-commit...
    }

    @Override
    public void preBegin(boolean readOnly) throws Exception {
        if (readOnly) return;
        
        if (readOnlyMode)
            throw new ReadOnlyException();
    }

    // UuidResolver methods (most are allowed to block)

    @Override
    public UuidDisplayName resolve(final String username) {
        return internalTransactionStrategy.execute(new TransactionCallback<UuidDisplayName>() {
            @Override
            public UuidDisplayName doInTransaction() throws Exception {
                Date expire = new Date(System.currentTimeMillis() - uuidCacheTimeout);
                UuidDisplayNameCache udnc = getEbeanServer().find(UuidDisplayNameCache.class).where()
                        .eq("name", username.toLowerCase())
                        .ge("timestamp", expire)
                        .findUnique();
                if (udnc != null) {
                    UUID uuid = udnc.getUuid();
                    return new UuidDisplayName(uuid, udnc.getDisplayName());
                }
                return null;
            }
        }, true /* always read-only */);
    }

    @Override
    public UuidDisplayName resolve(String username, boolean cacheOnly) {
        if (cacheOnly) return null; // No cache (because we ARE a cache) yet we must not block
        return resolve(username);
    }

    @Override
    public Map<String, UuidDisplayName> resolve(Collection<String> usernames) throws Exception {
        Map<String, UuidDisplayName> resolved = new LinkedHashMap<>();
        // Just resolve each name one at a time
        for (String username : usernames) {
            UuidDisplayName udn = resolve(username);
            if (udn != null) resolved.put(username.toLowerCase(), udn);
        }
        return resolved;
    }

    @Override
    public void preload(final String username, final UUID uuid) {
        if (readOnlyMode) return; // Do nothing if read-only

        // Must never block, so utilize our Executor
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                internalTransactionStrategy.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    public void doInTransactionWithoutResult() throws Exception {
                        // Does it already exist? (regardless of expiration)
                        UuidDisplayNameCache udnc = getEbeanServer().find(UuidDisplayNameCache.class).where()
                                .eq("name", username.toLowerCase())
                                .findUnique();
                        if (udnc == null) {
                            // If not, create it
                            udnc = new UuidDisplayNameCache();
                            udnc.setName(username.toLowerCase());
                        }
                        // Update values
                        udnc.setDisplayName(username);
                        udnc.setUuid(uuid);
                        udnc.setTimestamp(new Date());
                        getEbeanServer().save(udnc);
                    }
                });
            }
        });
    }

    @Override
    public void invalidate(final String username) {
        if (readOnlyMode)
            throw new ReadOnlyException();
        
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                internalTransactionStrategy.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    public void doInTransactionWithoutResult() throws Exception {
                        // If it exists (regardless of expiration), just delete it
                        UuidDisplayNameCache udnc = getEbeanServer().find(UuidDisplayNameCache.class).where()
                                .eq("name", username.toLowerCase())
                                .findUnique();
                        if (udnc != null)
                            getEbeanServer().delete(udnc);
                    }
                });
            }
        });
    }

    @Override
    public void invalidateAll() {
        if (readOnlyMode)
            throw new ReadOnlyException();

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                internalTransactionStrategy.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    public void doInTransactionWithoutResult() throws Exception {
                        // This is a bad way to do it, but I'm not sure how
                        // an SQL delete would work in light of configurable
                        // table names.
                        List<UuidDisplayNameCache> udncs = getEbeanServer().find(UuidDisplayNameCache.class)
                                .findList();
                        getEbeanServer().delete(udncs);
                    }
                });
            }
        });
    }

}
