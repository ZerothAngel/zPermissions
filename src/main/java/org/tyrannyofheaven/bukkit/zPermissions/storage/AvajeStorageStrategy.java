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

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.log;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.util.transaction.AsyncTransactionStrategy;
import org.tyrannyofheaven.bukkit.util.transaction.PreCommitHook;
import org.tyrannyofheaven.bukkit.util.transaction.RetryingAvajeTransactionStrategy;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.dao.AvajePermissionDao2;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.model.DataVersion;

/**
 * StorageStrategy for AvajePermissionDao2.
 * 
 * @author zerothangel
 */
public class AvajeStorageStrategy implements StorageStrategy, PreCommitHook {

    private final PermissionDao dao;

    private final AsyncTransactionStrategy transactionStrategy;

    private final TransactionStrategy retryingTransactionStrategy; // NB private and only used here for loading

    private final Plugin plugin;

    private final ExecutorService executorService;

    private final Lock loadLock = new ReentrantLock();

    private Long lastLoadedVersion = null; // protected by loadLock

    public AvajeStorageStrategy(Plugin plugin, int maxRetries) {
        // Following will be used to actually execute async
        executorService = Executors.newSingleThreadExecutor();

        transactionStrategy = new AsyncTransactionStrategy(new RetryingAvajeTransactionStrategy(plugin.getDatabase(), maxRetries, this), executorService);
        dao = new AvajePermissionDao2(plugin.getDatabase(), transactionStrategy.getExecutor());
        retryingTransactionStrategy = new RetryingAvajeTransactionStrategy(plugin.getDatabase(), maxRetries); // NB no need for pre-commit hook since it's read-only
        this.plugin = plugin;
    }

    @Override
    public void init() {
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
        return retryingTransactionStrategy.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                DataVersion currentVersion = getCurrentDataVersion();
                boolean doLoad;

                loadLock.lock();
                try {
                    doLoad = force || lastLoadedVersion == null || lastLoadedVersion != currentVersion.getVersion();
                }
                finally {
                    loadLock.unlock();
                }

                if (doLoad) {
                    ((AvajePermissionDao2)dao).load();
                    setLastLoadedVersion(currentVersion.getVersion());
                }
                
                return doLoad;
            }
        }, true);
    }

    @Override
    public PermissionDao getDao() {
        return dao;
    }

    @Override
    public TransactionStrategy getTransactionStrategy() {
        return transactionStrategy;
    }

    @Override
    public TransactionStrategy getRetryingTransactionStrategy() {
        return transactionStrategy;
    }

    // Return current data version. Will never return null.
    private DataVersion getCurrentDataVersion() {
        DataVersion dv = plugin.getDatabase().find(DataVersion.class).where()
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
        // Increment version and update timestamp
        dv.setVersion(dv.getVersion() + 1L);
        dv.setTimestamp(new Date());
        // Save
        plugin.getDatabase().save(dv);

        setLastLoadedVersion(dv.getVersion()); // probably not the appropriate place. Should really be post-commit...
    }

    private void setLastLoadedVersion(long version) {
        loadLock.lock();
        try {
            lastLoadedVersion = version;
        }
        finally {
            loadLock.unlock();
        }
    }

}
