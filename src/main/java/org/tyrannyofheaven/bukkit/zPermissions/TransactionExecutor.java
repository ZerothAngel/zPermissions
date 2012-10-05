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

import java.util.concurrent.Executor;

import com.avaje.ebean.EbeanServer;

/**
 * Simple Executor implementation that simply queues up Runnables within a
 * TransactionRunnable.
 * 
 * @author asaddi
 */
public class TransactionExecutor implements Executor {

    private final EbeanServer ebeanServer;
    
    private TransactionRunnable currentTransactionRunnable;

    public TransactionExecutor(EbeanServer ebeanServer) {
        this.ebeanServer = ebeanServer;
    }

    @Override
    public void execute(Runnable command) {
        if (currentTransactionRunnable == null)
            throw new IllegalStateException("No current TransactionRunnable");
        currentTransactionRunnable.addRunnable(command);
    }

    public void begin(int maxRetries) {
        if (currentTransactionRunnable != null)
            throw new IllegalStateException("Existing TransactionRunnable found");
        currentTransactionRunnable = new TransactionRunnable(ebeanServer, maxRetries);
    }

    public TransactionRunnable end() {
        if (currentTransactionRunnable == null)
            throw new IllegalStateException("No current TransactionRunnable");
        TransactionRunnable current = currentTransactionRunnable;
        currentTransactionRunnable = null;
        return current;
    }

}
