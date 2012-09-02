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
package org.tyrannyofheaven.bukkit.zPermissions;

import org.tyrannyofheaven.bukkit.util.transaction.AvajeTransactionStrategy;
import org.tyrannyofheaven.bukkit.util.transaction.RetryingAvajeTransactionStrategy;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.dao.AvajePermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionDao;

public class AvajeStorageStrategy implements StorageStrategy {

    private final PermissionDao dao;

    private final TransactionStrategy transactionStrategy;
    
    private final TransactionStrategy retryingTransactionStrategy;

    private final ZPermissionsPlugin plugin;

    AvajeStorageStrategy(ZPermissionsPlugin plugin, int maxRetries) {
        dao = new AvajePermissionDao(plugin.getDatabase());
        transactionStrategy = new AvajeTransactionStrategy(plugin.getDatabase());
        retryingTransactionStrategy = new RetryingAvajeTransactionStrategy(plugin.getDatabase(), maxRetries);
        this.plugin = plugin;
    }

    @Override
    public void init() {
        plugin.createDatabaseSchema();
        plugin.applyCacheSettings();
    }

    @Override
    public void shutdown() {
        // Nothing to do
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
        return retryingTransactionStrategy;
    }

}
