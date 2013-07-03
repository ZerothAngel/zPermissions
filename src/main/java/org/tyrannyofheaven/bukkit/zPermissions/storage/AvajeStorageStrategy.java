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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.util.transaction.AsyncTransactionStrategy;
import org.tyrannyofheaven.bukkit.util.transaction.RetryingAvajeTransactionStrategy;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.dao.AvajePermissionDao2;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionDao;

/**
 * StorageStrategy for AvajePermissionDao2.
 * 
 * @author zerothangel
 */
public class AvajeStorageStrategy implements StorageStrategy {

    private final PermissionDao dao;

    private final AsyncTransactionStrategy transactionStrategy;

    private final TransactionStrategy retryingTransactionStrategy;

    private final Plugin plugin;

    private final ExecutorService executorService;

    public AvajeStorageStrategy(Plugin plugin, int maxRetries) {
        // Following will be used to actually execute async
        executorService = Executors.newSingleThreadExecutor();

        transactionStrategy = new AsyncTransactionStrategy(new RetryingAvajeTransactionStrategy(plugin.getDatabase(), maxRetries), executorService);
        dao = new AvajePermissionDao2(plugin.getDatabase(), transactionStrategy.getExecutor());
        retryingTransactionStrategy = new RetryingAvajeTransactionStrategy(plugin.getDatabase(), maxRetries);
        this.plugin = plugin;
    }

    @Override
    public void init() {
        log(plugin, "Loading all permissions from database...");
//        plugin.getDatabase().getAdminLogging().setDebugGeneratedSql(true);
        long start = System.currentTimeMillis();
        refreshInternal(); // synchronously
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
    public void refresh(final Runnable finishTask) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                refreshInternal();
                
                if (finishTask != null)
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, finishTask);
            }
        });
    }

    private void refreshInternal() {
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
