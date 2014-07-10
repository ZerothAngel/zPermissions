/*
 * Copyright 2014 ZerothAngel <zerothangel@tyrannyofheaven.org>
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
 * Data-access interface for zPermissions. Mainly concerned with CUD operations
 * (that is, CRUD without the R).
 * 
 * @author zerothangel
 */
public interface PermissionDao {

    public void createRegion(PermissionRegion region);

    public void createWorld(PermissionWorld world);

    public void createEntity(PermissionEntity entity);

    public void createOrUpdateEntry(Entry entry);

    public void deleteEntry(Entry entry);

    public void createOrUpdateMembership(Membership membership);

    public void setEntityParent(PermissionEntity entity, PermissionEntity parent);

    public void createOrUpdateInheritance(Inheritance inheritance);

    public void deleteInheritance(Inheritance inheritance);

    public void setEntityPriority(PermissionEntity entity, int priority);

    public void deleteRegions(Collection<PermissionRegion> regions);

    public void deleteWorlds(Collection<PermissionWorld> worlds);

    public void deleteEntity(PermissionEntity entity);

    public void deleteMembership(Membership membership);

    public void createOrUpdateMetadata(EntityMetadata metadata);

    public void deleteMetadata(EntityMetadata metadata);

    public void updateDisplayName(PermissionEntity entity);

    public void updateDisplayName(Membership membership);

}