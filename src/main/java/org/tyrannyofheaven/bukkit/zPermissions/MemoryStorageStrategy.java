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
package org.tyrannyofheaven.bukkit.zPermissions;

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.debug;
import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.log;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

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

    private static final int SAVE_DELAY = 200; // 10 seconds

    private final MemoryPermissionDao dao = new MemoryPermissionDao();

    private final ZPermissionsPlugin plugin;

    private final File saveFile;

    private boolean initialized;

    private int saveTask = -1;

    MemoryStorageStrategy(ZPermissionsPlugin plugin, File saveFile) {
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

        try {
            dao.save(saveFile);
        }
        catch (IOException e) {
            log(plugin, Level.SEVERE, "Error saving permissions database:", e);
        }
    }

    @Override
    public void refresh(Runnable finishTask) {
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
        if (callback == null)
            throw new IllegalArgumentException("callback cannot be null");
        try {
            T result = callback.doInTransaction();
            // Schedule a save if dirty and no pending save
            if (dao.isDirty() && saveTask < 0) {
                saveTask = plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, this, SAVE_DELAY).getTaskId();
                if (saveTask < 0)
                    log(plugin, Level.SEVERE, "Error scheduling permissions database save task");
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
        try {
            dao.save(saveFile);
        }
        catch (IOException e) {
            log(plugin, Level.SEVERE, "Error saving permissions database:", e);
        }
    }

}
