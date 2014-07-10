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
package org.tyrannyofheaven.bukkit.zPermissions.dao;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;

import org.tyrannyofheaven.bukkit.zPermissions.model.EntityMetadata;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Inheritance;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

import com.avaje.ebean.EbeanServer;

/**
 * Avaje PermissionService implementation that keeps everything in memory.
 * 
 * @author asaddi
 */
public class AvajePermissionService extends NonBlockingPermissionService {

    private final AvajePermissionDao permissionDao;

    public AvajePermissionService(EbeanServer ebeanServer, Executor executor) {
        permissionDao = new AvajePermissionDao(ebeanServer, executor);
        setPermissionDao(permissionDao);
    }

    private EbeanServer getEbeanServer() {
        return permissionDao.getEbeanServer();
    }

    public void load() {
        // Current rationale: On any given server, the number of groups will have
        // an upper bound. However, the number of players will not. Granted, most
        // players will simply be members and not full-blown entities themselves.
        // For now, join entities and permissions when fetching players.
        List<PermissionEntity> players = getEbeanServer().createQuery(PermissionEntity.class,
                "find PermissionEntity fetch permissions where group = false")
                .findList();
        // But do not bother for groups.
        List<PermissionEntity> groups = getEbeanServer().createQuery(PermissionEntity.class,
                "find PermissionEntity fetch parent (displayName) where group = true")
                .findList();
        load(players, groups);
    }

    private void load(List<PermissionEntity> players, List<PermissionEntity> groups) {
        MemoryState memoryState = new MemoryState();

        // Create full copies to force lazy-loads
        for (PermissionEntity player : players) {
            PermissionEntity newPlayer = getEntity(memoryState, player.getDisplayName(), player.getUuid(), false);
            loadPermissions(memoryState, player.getPermissions(), newPlayer);
            loadMetadata(getEbeanServer().find(EntityMetadata.class).where()
                    .eq("entity", player)
                    .findList(), newPlayer);
        }
        for (PermissionEntity group : groups) {
            PermissionEntity newGroup = getEntity(memoryState, group.getDisplayName(), null, true);
            loadPermissions(memoryState, getEbeanServer().find(Entry.class).where()
                    .eq("entity", group)
                    .findList(), newGroup);
            loadMetadata(getEbeanServer().find(EntityMetadata.class).where()
                    .eq("entity", group)
                    .findList(), newGroup);
            newGroup.setPriority(group.getPriority());
            if (group.getParent() != null) {
                // Backwards compatibility
                PermissionEntity parentEntity = getEntity(memoryState, group.getParent().getDisplayName(), null, true);

                Inheritance newInheritance = new Inheritance();
                newInheritance.setChild(newGroup);
                newInheritance.setParent(parentEntity);
                newInheritance.setOrdering(0);
                
                // Linkages
                newGroup.getInheritancesAsChild().add(newInheritance);
                parentEntity.getInheritancesAsParent().add(newInheritance);
            }
            else {
                List<Inheritance> inheritances = getEbeanServer().find(Inheritance.class).where()
                        .eq("child", group)
                        .join("parent", "displayName")
                        .findList();
                for (Inheritance inheritance : inheritances) {
                    PermissionEntity parentEntity = getEntity(memoryState, inheritance.getParent().getDisplayName(), null, true);

                    Inheritance newInheritance = new Inheritance();
                    newInheritance.setChild(newGroup);
                    newInheritance.setParent(parentEntity);
                    newInheritance.setOrdering(inheritance.getOrdering());
                    
                    // Linkages
                    newGroup.getInheritancesAsChild().add(newInheritance);
                    parentEntity.getInheritancesAsParent().add(newInheritance);
                }
            }
            List<Membership> memberships = getEbeanServer().find(Membership.class).where()
                    .eq("group", group)
                    .findList();
            for (Membership membership : memberships) {
                Membership newMembership = new Membership();
                newMembership.setMember(membership.getMember().toLowerCase());
                newMembership.setDisplayName(membership.getDisplayName());
                newMembership.setGroup(newGroup);
                newMembership.setExpiration(membership.getExpiration());
                newGroup.getMemberships().add(newMembership);
                
                rememberMembership(memoryState, newMembership);
            }
        }
        
        setMemoryState(memoryState);
    }

    private void loadPermissions(MemoryState memoryState, Collection<Entry> permissions, PermissionEntity entity) {
        for (Entry entry : permissions) {
            Entry newEntry = new Entry();

            newEntry.setRegion(entry.getRegion() == null ? null : getRegion(memoryState, entry.getRegion().getName()));
            newEntry.setWorld(entry.getWorld() == null ? null : getWorld(memoryState, entry.getWorld().getName()));
            newEntry.setPermission(entry.getPermission().toLowerCase());
            newEntry.setValue(entry.isValue());

            newEntry.setEntity(entity);
            entity.getPermissions().add(newEntry);
        }
    }

    private void loadMetadata(Collection<EntityMetadata> metadata, PermissionEntity entity) {
        for (EntityMetadata em : metadata) {
            EntityMetadata newMetadata = new EntityMetadata();

            newMetadata.setName(em.getName().toLowerCase());
            newMetadata.setValue(em.getValue());

            newMetadata.setEntity(entity);
            entity.getMetadata().add(newMetadata);
        }
        
        entity.updateMetadataMap();
    }

}
