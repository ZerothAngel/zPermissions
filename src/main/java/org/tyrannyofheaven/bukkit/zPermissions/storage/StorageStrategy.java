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

import java.util.Map;

import org.tyrannyofheaven.bukkit.util.transaction.TransactionStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionService;

/**
 * Encompasses initialization/shutdown and all other aspects of permissions
 * data persistence.
 * 
 * @author zerothangel
 */
public interface StorageStrategy {

    /**
     * Perform any required initialization. Called once from the zPermissions
     * onEnable() handler.
     * 
     * @param configMap Configuration map containing key-value pairs
     */
    public void init(Map<String, Object> configMap);
    
    /**
     * Perform any required cleanup. Called from the zPermissions onDisable()
     * handler.
     */
    public void shutdown();

    /**
     * A request to re-read from the permissions store. Whether this is actually
     * done is implementation-dependent.
     * 
     * @param force A hint on whether or not to force a refresh. For conditional
     *   refreshes, this will be <code>false</code>.
     * @param finishTask If permissions were actually re-read and finishTask is
     *   not-<code>null</code>, this task should be run <i>synchronously</i>
     *   in the main thread.
     */
    public void refresh(boolean force, Runnable finishTask);

    /**
     * Retrieve an instance of {@link PermissionService} that is associated with
     * this storage strategy.
     * 
     * @return the PermissionService instance
     */
    public PermissionService getPermissionService();
    
    /**
     * Retrieve an instance of {@link TransactionStrategy} that is associated
     * with this storage strategy. This version is typically used for
     * read-only transactions or transactions that are not safe to retry
     * (because of being poorly written :P).
     * 
     * @return a TransactionStrategy
     */
    public TransactionStrategy getTransactionStrategy();

    /**
     * Retrieve an instance of {@link TransactionStrategy} that is associated
     * with this storage strategy. This version should support retries if it
     * makes sense (e.g. SQL transactions with rollback). Typical used with
     * simple operations (single PermissionService method call).
     * 
     * @return a TransactionStrategy
     */
    public TransactionStrategy getRetryingTransactionStrategy();

}
