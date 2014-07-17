// Released to the public domain
package org.tyrannyofheaven.bukkit.zPermissions.dao;

import java.util.Collection;

import org.tyrannyofheaven.bukkit.zPermissions.model.EntityMetadata;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Inheritance;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionRegion;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionWorld;

/*
 * Fairly straightforward, the PermissionDao is responsible for all CUD (Create,
 * Update, Delete) operations. Reading can also be handled here, but since reading
 * always involves reading the full store in one shot, it normally falls under
 * the purview of the matching StorageStrategy's init() & refresh() methods.
 * 
 * There are a few rules to follow when implementing each method:
 * 1. NEVER block. If you must perform blocking I/O, do it asynchronously.
 * 2. Never modify the passed-in object.
 * 3. You may want to make a copy of any pertinent properties of the passed-in
 *    object. It may not be wise to hold on to it, especially if acting async.
 * 
 * There are at least two styles of implementation:
 * 1. Implement each method normally, e.g. createEntity() will actually perform
 *    I/O to create that entity somewhere. AvajePermissionDao is an example.
 * 2. Each method simply sets a dirty flag. An asynchronous thread periodically
 *    checks the dirty flag, and if dirty, will call InMemoryPermissionService's
 *    getPlayer()/getGroups()/getWorlds()/getRegions() methods to get a snapshot
 *    of the current state which is then written out somewhere. FilePermissionDao
 *    is an example.
 *    
 *    Note, getPlayer()/getGroups() etc. should be in a single synchronized block,
 *    synchronized on the InMemoryPermissionService instance. Also note that
 *    the returned collections and objects by getPlayer() etc. MUST NEVER be
 *    modified.
 * 
 * Note, createRegion(), createWorld(), createEntity() should do nothing if the
 * object in question already exists.
 */
public class DummyPermissionDao implements PermissionDao {

    @Override
    public void createRegion(PermissionRegion region) {
    }

    @Override
    public void createWorld(PermissionWorld world) {
    }

    @Override
    public void createEntity(PermissionEntity entity) {
    }

    @Override
    public void createOrUpdateEntry(Entry entry) {
    }

    @Override
    public void deleteEntry(Entry entry) {
    }

    @Override
    public void createOrUpdateMembership(Membership membership) {
    }

    @Override
    public void setEntityParent(PermissionEntity entity, PermissionEntity parent) {
    }

    @Override
    public void createOrUpdateInheritance(Inheritance inheritance) {
    }

    @Override
    public void deleteInheritance(Inheritance inheritance) {
    }

    @Override
    public void setEntityPriority(PermissionEntity entity, int priority) {
    }

    @Override
    public void deleteRegions(Collection<PermissionRegion> regions) {
    }

    @Override
    public void deleteWorlds(Collection<PermissionWorld> worlds) {
    }

    @Override
    public void deleteEntity(PermissionEntity entity) {
    }

    @Override
    public void deleteMembership(Membership membership) {
    }

    @Override
    public void createOrUpdateMetadata(EntityMetadata metadata) {
    }

    @Override
    public void deleteMetadata(EntityMetadata metadata) {
    }

    @Override
    public void updateDisplayName(PermissionEntity entity) {
    }

    @Override
    public void updateDisplayName(Membership membership) {
    }

}
