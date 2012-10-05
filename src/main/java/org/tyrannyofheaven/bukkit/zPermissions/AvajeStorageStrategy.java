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
import org.tyrannyofheaven.bukkit.util.transaction.AsyncTransactionStrategy;
import org.tyrannyofheaven.bukkit.util.transaction.RetryingAvajeTransactionStrategy;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.dao.AvajePermissionDao2;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionDao;

/**
 * StorageStrategy for AvajePermissionDao2.
 * 
 * @author asaddi
 */
public class AvajeStorageStrategy implements StorageStrategy {

    private final PermissionDao dao;

    private final AsyncTransactionStrategy transactionStrategy;

    private final TransactionStrategy retryingTransactionStrategy;

    private final ZPermissionsPlugin plugin;

    private final ExecutorService executorService;

    AvajeStorageStrategy(ZPermissionsPlugin plugin, int maxRetries) {
        // Following will be used to actually execute async
        executorService = Executors.newSingleThreadExecutor();

        transactionStrategy = new AsyncTransactionStrategy(new RetryingAvajeTransactionStrategy(plugin.getDatabase(), maxRetries), executorService);
        dao = new AvajePermissionDao2(plugin.getDatabase(), transactionStrategy.getExecutor());
        retryingTransactionStrategy = new RetryingAvajeTransactionStrategy(plugin.getDatabase(), maxRetries);
        this.plugin = plugin;
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
        return transactionStrategy;
    }

    @Override
    public TransactionStrategy getRetryingTransactionStrategy() {
        return transactionStrategy;
    }

}
