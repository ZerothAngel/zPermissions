/*
 * Copyright 2014 Allan Saddi <allan@saddi.com>
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
package org.tyrannyofheaven.bukkit.zPermissions.dao;

import java.util.Collection;

import org.tyrannyofheaven.bukkit.zPermissions.model.EntityMetadata;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Inheritance;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionRegion;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionWorld;

/**
 * Flat-file based PermissionDao implementation. Merely sets the dirty flag
 * when any of the (CUD) DAO methods are called.
 * 
 * @author asaddi
 */
public class FilePermissionDao implements PermissionDao {

    private boolean dirty;

    synchronized boolean isDirty() {
        return dirty;
    }

    private void setDirty() {
        setDirty(true);
    }

    private synchronized void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    synchronized void clearDirty() {
        this.dirty = false;
    }

    @Override
    public void createRegion(PermissionRegion region) {
        setDirty();
    }

    @Override
    public void createWorld(PermissionWorld world) {
        setDirty();
    }

    @Override
    public void createEntity(PermissionEntity entity) {
        setDirty();
    }

    @Override
    public void createOrUpdateEntry(Entry entry) {
        setDirty();
    }

    @Override
    public void deleteEntry(Entry entry) {
        setDirty();
    }

    @Override
    public void createOrUpdateMembership(Membership membership) {
        setDirty();
    }

    @Override
    public void deleteEntity(PermissionEntity entity) {
        setDirty();
    }

    @Override
    public void deleteMembership(Membership membership) {
        setDirty();
    }

    @Override
    public void setEntityParent(PermissionEntity entity, PermissionEntity parent) {
        setDirty();
    }

    @Override
    public void setEntityPriority(PermissionEntity entity, int priority) {
        setDirty();
    }

    @Override
    public void deleteRegions(Collection<PermissionRegion> regions) {
        setDirty();
    }

    @Override
    public void deleteWorlds(Collection<PermissionWorld> worlds) {
        setDirty();
    }

    @Override
    public void createOrUpdateMetadata(EntityMetadata metadata) {
        setDirty();
    }

    @Override
    public void deleteMetadata(EntityMetadata metadata) {
        setDirty();
    }

    @Override
    public void createOrUpdateInheritance(Inheritance inheritance) {
        setDirty();
    }

    @Override
    public void deleteInheritance(Inheritance inheritance) {
        setDirty();
    }

    @Override
    public void updateDisplayName(PermissionEntity entity) {
        setDirty();
    }

    @Override
    public void updateDisplayName(Membership membership) {
        setDirty();
    }

}
