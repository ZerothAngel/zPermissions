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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bukkit.Bukkit;
import org.tyrannyofheaven.bukkit.util.transaction.RetryingAvajeTransactionStrategy;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionException;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.dao.AvajePermissionDao2;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionDao;

public class AvajeStorageStrategy implements StorageStrategy, TransactionStrategy {

    private final TransactionExecutor transactionExecutor;

    private final PermissionDao dao;

    private final TransactionStrategy retryingTransactionStrategy;

    private final ZPermissionsPlugin plugin;

    private final int maxRetries;

    private final ExecutorService executorService;

    AvajeStorageStrategy(ZPermissionsPlugin plugin, int maxRetries) {
        transactionExecutor = new TransactionExecutor(plugin.getDatabase());
        dao = new AvajePermissionDao2(plugin.getDatabase(), transactionExecutor);
        retryingTransactionStrategy = new RetryingAvajeTransactionStrategy(plugin.getDatabase(), maxRetries);
        this.plugin = plugin;
        this.maxRetries = maxRetries;
        
        // Following will be used to actually execute async
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void init() {
        plugin.createDatabaseSchema();
        plugin.applyCacheSettings();
//        plugin.getDatabase().getAdminLogging().setDebugGeneratedSql(true);
        refreshInternal(); // synchronously
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }

    @Override
    public void refresh(final Runnable finishTask) {
        Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                refreshInternal();
                
                if (finishTask != null)
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, finishTask);
            }
        });
    }

    // Might share TransactionStrategy, so synchronize
    synchronized private void refreshInternal() {
        retryingTransactionStrategy.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                ((AvajePermissionDao2)dao).load();
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
        if (callback == null)
            throw new IllegalArgumentException("callback cannot be null");
        try {
            // Start collecting runnables
            transactionExecutor.begin(maxRetries); // always use maxRetries since these are only writes
            boolean success = false; // so we know we executed callback successfully
            try {
                T result = callback.doInTransaction();
                success = true;
                return result;
            }
            finally {
                TransactionRunnable transactionRunnable = transactionExecutor.end();
                if (!transactionRunnable.isEmpty() && success) {
                    // Got something, execute it async
                    executorService.execute(transactionRunnable);
                }
            }
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

}
