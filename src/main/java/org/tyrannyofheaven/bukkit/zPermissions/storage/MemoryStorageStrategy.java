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
package org.tyrannyofheaven.bukkit.zPermissions.storage;

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.debug;
import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionException;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.dao.MemoryPermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionDao;

/**
 * StorageStrategy for MemoryPermissionDao.
 * 
 * @author asaddi
 */
public class MemoryStorageStrategy implements StorageStrategy, TransactionStrategy, Runnable {

    private static final int SAVE_DELAY = 10; // seconds

    private final MemoryPermissionDao dao = new MemoryPermissionDao();

    private final Plugin plugin;

    private final File saveFile;

    private boolean initialized;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> saveTask; // protected by this

    public MemoryStorageStrategy(Plugin plugin, File saveFile) {
        this.plugin = plugin;
        this.saveFile = saveFile;
    }

    @Override
    public void init() {
        if (saveFile.exists()) {
            try {
                dao.load(saveFile);
                initialized = true;
            }
            catch (IOException e) {
                log(plugin, Level.SEVERE, "Error loading permissions database:", e);
            }
        }
        else {
            initialized = true;
        }
    }

    @Override
    public void shutdown() {
        if (!initialized) return; // if didn't load properly DON'T overwrite

        // Kill scheduled async task
        cancelSaveTask();

        debug(plugin, "Saving permissions database one last time...");
        // Queue up a final save task
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                save();
            }
        });
        // Start shutting down
        executorService.shutdown();
        // And wait
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
    public void refresh(boolean force, final Runnable finishTask) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                cancelSaveTask();

                try {
                    dao.load(saveFile);
                }
                catch (IOException e) {
                    log(plugin, Level.SEVERE, "Error loading permissions database:", e);
                }
                
                if (finishTask != null)
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, finishTask);
            }
        });
    }

    @Override
    public PermissionDao getDao() {
        return dao;
    }

    @Override
    public TransactionStrategy getTransactionStrategy() {
        return this;
    }

    @Override
    public TransactionStrategy getRetryingTransactionStrategy() {
        return this;
    }

    @Override
    public <T> T execute(TransactionCallback<T> callback) {
        return execute(callback, false);
    }

    @Override
    public <T> T execute(TransactionCallback<T> callback, boolean readOnly) {
        if (callback == null)
            throw new IllegalArgumentException("callback cannot be null");
        try {
            T result = callback.doInTransaction();
            // Schedule a save if dirty and no pending save
            if (dao.isDirty()) {
                synchronized (this) {
                    if (dao.isDirty() && saveTask == null) { // NB re-test dirty
                        saveTask = executorService.schedule(this, SAVE_DELAY, TimeUnit.SECONDS);
                    }
                }
            }
            return result;
        }
        catch (Error | RuntimeException e) {
            // No need to wrap these, just re-throw
            throw e;
        }
        catch (Throwable t) {
            throw new TransactionException(t);
        }
    }

    @Override
    public void run() {
        debug(plugin, "Auto-saving permissions database...");
        save();
        synchronized (this) {
            saveTask = null;
        }
    }

    // NB Only called from executorService, which is single-threaded
    // All calls are therefore serialized
    private void save() {
        try {
            dao.save(saveFile);
        }
        catch (IOException e) {
            log(plugin, Level.SEVERE, "Error saving permissions database:", e);
        }
    }

    private void cancelSaveTask() {
        synchronized (this) {
            if (saveTask != null) {
                saveTask.cancel(false);
            }
            saveTask = null;
        }
    }

}
