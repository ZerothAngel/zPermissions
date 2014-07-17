// Released to the public domain
package org.tyrannyofheaven.bukkit.zPermissions.storage;

import java.util.Map;

import org.tyrannyofheaven.bukkit.util.transaction.NullTransactionStrategy;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.dao.DummyPermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.dao.InMemoryPermissionService;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionService;

/*
 * This is the top-level storage interface. A custom implementation can be used
 * by setting the custom-storage-strategy option in zPermissions' config.yml.
 * 
 * To be used this way, this class must have a no-arg constructor.
 * 
 * Note that init() and refresh() are usually responsible for loading from
 * whatever store you use. However, loading can be delegated to the PermissionDao
 * implementation, if it makes more sense to do it that way (and it usually does).
 * 
 * Loading usually consists of:
 * 1. Instantiating a new MemoryState instance
 * 2. Using the static methods in InMemoryPermissionService to create entities
 *    (and filling out their permissions and metadata) and memberships. Note,
 *    letter case matters for certain properties: Entry#permission, EntityMetadata#name,
 *    Membership#member. When in doubt, check the loading code in AvajePermissionDao &
 *    FilePermissionDao.
 * 3. Calling your InMemoryPermissionService instance's setMemoryState() method.
 * 
 * Also see javadocs for StorageStrategy.
 */
public class DummyStorageStrategy implements StorageStrategy {

    private final InMemoryPermissionService permissionService = new InMemoryPermissionService();

    private final DummyPermissionDao permissionDao = new DummyPermissionDao(); // You may want to pass permissionService to the constructor

    private final TransactionStrategy transactionStrategy = new NullTransactionStrategy();

    public DummyStorageStrategy() {
        permissionService.setPermissionDao(permissionDao);
    }

    @Override
    public void init(Map<String, Object> configMap) {
        /*
         * Perform initialization. Feel free to use blocking I/O.
         * Simple implementations may simply call refresh(true, null)
         */
    }

    @Override
    public void shutdown() {
        /*
         * Perform cleanup or final I/O. Can also block.
         */
    }

    @Override
    public void refresh(boolean force, Runnable finishTask) {
        /*
         * Normal calls to this should NEVER block. (However, if called from init()
         * above, it should block so initialization does not continue until
         * everything has loaded. See AvajeStorageStrategy for an example of how
         * this is handled.)
         * 
         * Only execute finishTask if in-memory representation changed
         * (i.e. InMemoryPermissionService#setMemoryState() was called) and
         * finishTask != null
         *
         * If you do execute finishTask, remember the contract dictates that it
         * must execute in the main thread.
         */
    }

    @Override
    public PermissionService getPermissionService() {
        /*
         * Should return your PermissionService implementation, which should
         * basically be an InMemoryPermissionService instance (unless you
         * feel like re-implementing THAT interface)
         */
        return permissionService;
    }

    /*
     * In the olden days (pre-1.0), there were two distinct TransactionStrategy
     * instances. However, if you use InMemoryPermissionService as your
     * PermissionService implementation, the distinction doesn't matter anymore.
     *
     * Both getters should return the same instance, and it will usually be
     * one of two implementations:
     * 
     * NullTransactionStrategy - If you don't actually do any I/O in PermissionDao, e.g.
     *   you just set a dirty flag and perform I/O elsewhere (similar to FilePermissionDao).
     * AsyncTransactionStrategy - If your store is transactional (e.g. requires a real
     *   TransactionStrategy implementation) and your I/O would normally block.
     *   Databases fall under this. (Also see AvajePermissionDao.)
     * 
     * If your I/O is blocking but not transactional, you can probably get away
     * with using NullTransactionStrategy.
     */

    @Override
    public TransactionStrategy getTransactionStrategy() {
        return transactionStrategy;
    }

    @Override
    public TransactionStrategy getRetryingTransactionStrategy() {
        return transactionStrategy;
    }

}
