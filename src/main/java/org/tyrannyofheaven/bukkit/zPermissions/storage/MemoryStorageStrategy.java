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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
 * @author zerothangel
 */
public class MemoryStorageStrategy implements StorageStrategy, TransactionStrategy, Runnable {

    private static final int SAVE_DELAY = 200; // 10 seconds

    private final MemoryPermissionDao dao = new MemoryPermissionDao();

    private final Plugin plugin;

    private final File saveFile;

    private boolean initialized;

    private int saveTask = -1; // NB synchronized on this

    private final Lock saveLock = new ReentrantLock();

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
        synchronized (this) {
            if (saveTask > -1) {
                // NB still race condition, but saveLock will serialize saves
                Bukkit.getScheduler().cancelTask(saveTask);
                saveTask = -1;
            }
        }

        debug(plugin, "Saving permissions database one last time...");
        save();
    }

    @Override
    public void refresh(boolean force, Runnable finishTask) {
        // Do nothing
        if (finishTask != null)
            finishTask.run();
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
                    if (saveTask < 0) {
                        saveTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this, SAVE_DELAY).getTaskId();
                        if (saveTask < 0)
                            log(plugin, Level.SEVERE, "Error scheduling permissions database save task");
                    }
                }
            }
            return result;
        }
        catch (Error e) {
            throw e;
        }
        catch (RuntimeException e) {
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
        synchronized (this) {
            saveTask = -1;
        }
        save();
    }

    private void save() {
        try {
            saveLock.lock();
            try {
                dao.save(saveFile);
            }
            finally {
                saveLock.unlock();
            }
        }
        catch (IOException e) {
            log(plugin, Level.SEVERE, "Error saving permissions database:", e);
        }
    }

}
