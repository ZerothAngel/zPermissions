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

import org.tyrannyofheaven.bukkit.util.transaction.TransactionStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionDao;

/**
 * Encompasses initialization/shutdown and all other aspects of permissions
 * data persistence.
 * 
 * @author asaddi
 */
public interface StorageStrategy {

    public void init();
    
    public void shutdown();
    
    public void refresh(Runnable finishTask);

    public PermissionDao getDao();
    
    public TransactionStrategy getTransactionStrategy();
    
    public TransactionStrategy getRetryingTransactionStrategy();

}
