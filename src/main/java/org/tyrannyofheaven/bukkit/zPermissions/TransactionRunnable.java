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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import org.tyrannyofheaven.bukkit.util.transaction.TransactionException;

import com.avaje.ebean.EbeanServer;

/**
 * A Runnable that holds a sequence of writing Runnables to be executed
 * transactionally.
 * 
 * @author zerothangel
 */
public class TransactionRunnable implements Runnable {

    private final Logger logger = Logger.getLogger(getClass().getName());

    private final EbeanServer ebeanServer;

    private final List<Runnable> runnables = new ArrayList<Runnable>();

    private int maxRetries;

    public TransactionRunnable(EbeanServer ebeanServer) {
        this(ebeanServer, 0);
    }

    public TransactionRunnable(EbeanServer ebeanServer, int maxRetries) {
        this.ebeanServer = ebeanServer;
        this.maxRetries = maxRetries;
    }

    public void addRunnable(Runnable runnable) {
        runnables.add(runnable);
    }

    public boolean isEmpty() {
        return runnables.isEmpty();
    }

    private EbeanServer getEbeanServer() {
        return ebeanServer;
    }
    
    private List<Runnable> getRunnables() {
        return runnables;
    }

    @Override
    public void run() {
        try {
            runWithRetry();
        }
        catch (Error e) {
            throw e;
        }
        catch (Throwable t) {
            logger.log(Level.SEVERE, "Error writing to database", t);
        }
    }

    private void runWithRetry() {
        PersistenceException savedPE = null;
        for (int attempt = -1; attempt < maxRetries; attempt++) { // yeah, really hope Runnables are re-runnable...
            try {
                getEbeanServer().beginTransaction();
                try {
                    for (Runnable runnable : getRunnables()) {
                        runnable.run();
                    }
                    getEbeanServer().commitTransaction();
                    return;
                }
                finally {
                    getEbeanServer().endTransaction();
                }
            }
            catch (Error e) {
                throw e;
            }
            catch (PersistenceException e) {
                savedPE = e;
                continue;
            }
            catch (RuntimeException e) {
                // No need to wrap these, just re-throw
                throw e;
            }
            catch (Throwable t) {
                throw new TransactionException(t);
            }
        }

        // At this point, we've run out of attempts
        throw savedPE;
    }

}
