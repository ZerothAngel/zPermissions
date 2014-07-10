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

import static org.tyrannyofheaven.bukkit.zPermissions.dao.InMemoryPermissionService.getEntity;
import static org.tyrannyofheaven.bukkit.zPermissions.dao.InMemoryPermissionService.getRegion;
import static org.tyrannyofheaven.bukkit.zPermissions.dao.InMemoryPermissionService.getWorld;
import static org.tyrannyofheaven.bukkit.zPermissions.dao.InMemoryPermissionService.rememberMembership;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.tyrannyofheaven.bukkit.zPermissions.dao.InMemoryPermissionService.MemoryState;
import org.tyrannyofheaven.bukkit.zPermissions.model.EntityMetadata;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Inheritance;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionRegion;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionWorld;

import com.avaje.ebean.EbeanServer;

/**
 * Avaje PermissionDao implementation. All database operations are handed off
 * to the given Executor.
 * 
 * @author zerothangel
 */
public class AvajePermissionDao implements PermissionDao {

    private final Logger logger = Logger.getLogger(getClass().getName());

    private final InMemoryPermissionService permissionService;

    private final EbeanServer ebeanServer;

    private final Executor executor;

    public AvajePermissionDao(InMemoryPermissionService permissionService, EbeanServer ebeanServer, Executor executor) {
        this.permissionService = permissionService;
        this.ebeanServer = ebeanServer;
        this.executor = executor != null ? executor : new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
    }

    EbeanServer getEbeanServer() {
        return ebeanServer;
    }

    private Executor getExecutor() {
        return executor;
    }

    @Override
    public void createRegion(PermissionRegion region) {
        final String name = region.getName().toLowerCase();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                PermissionRegion dbRegion = getEbeanServer().find(PermissionRegion.class).where()
                        .eq("name", name)
                        .findUnique();
                if (dbRegion == null) {
                    dbRegion = new PermissionRegion();
                    dbRegion.setName(name);
                    getEbeanServer().save(dbRegion);
                }
            }
        });
    }

    @Override
    public void createWorld(PermissionWorld world) {
        final String name = world.getName().toLowerCase();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                PermissionWorld dbWorld = getEbeanServer().find(PermissionWorld.class).where()
                        .eq("name", name)
                        .findUnique();
                if (dbWorld == null) {
                    dbWorld = new PermissionWorld();
                    dbWorld.setName(name);
                    getEbeanServer().save(dbWorld);
                }
            }
        });
    }

    @Override
    public void createEntity(PermissionEntity entity) {
        final String name = entity.getName();
        final String displayName = entity.getDisplayName();
        final boolean group = entity.isGroup();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                final String lname = name.toLowerCase();
                PermissionEntity dbEntity = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", lname)
                        .eq("group", group)
                        .findUnique();
                if (dbEntity == null) {
                    dbEntity = new PermissionEntity();
                    dbEntity.setName(lname);
                    dbEntity.setGroup(group);
                    dbEntity.setDisplayName(displayName);
                    // NB assumes name/group/displayName are only attributes that need saving
                    getEbeanServer().save(dbEntity);
                }
            }
        });
    }

    @Override
    public void createOrUpdateEntry(Entry entry) {
        final String name = entry.getEntity().getName();
        final boolean group = entry.getEntity().isGroup();
        final String regionName = entry.getRegion() == null ? null : entry.getRegion().getName();
        final String worldName = entry.getWorld() == null ? null : entry.getWorld().getName();
        final String permission = entry.getPermission().toLowerCase();
        final boolean value = entry.isValue();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Locate dependent objects
                PermissionEntity entity = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", name.toLowerCase())
                        .eq("group", group)
                        .findUnique();
                if (entity == null) {
                    entity = inconsistentEntity(name, group);
                }

                PermissionRegion region = null;
                if (regionName != null) {
                    region = getEbeanServer().find(PermissionRegion.class).where()
                            .eq("name", regionName.toLowerCase())
                            .findUnique();
                    if (region == null) {
                        region = inconsistentRegion(regionName);
                    }
                }
                
                PermissionWorld world = null;
                if (worldName != null) {
                    world = getEbeanServer().find(PermissionWorld.class).where()
                            .eq("name", worldName)
                            .findUnique();
                    if (world == null) {
                        world = inconsistentWorld(worldName);
                    }
                }
                
                Entry dbEntry = getEbeanServer().find(Entry.class).where()
                        .eq("entity", entity)
                        .eq("region", region)
                        .eq("world", world)
                        .eq("permission", permission)
                        .findUnique();
                if (dbEntry == null) {
                    dbEntry = new Entry();
                    dbEntry.setEntity(entity);
                    dbEntry.setRegion(region);
                    dbEntry.setWorld(world);
                    dbEntry.setPermission(permission);
                }
                
                dbEntry.setValue(value);
                getEbeanServer().save(dbEntry);
            }
        });
    }

    @Override
    public void deleteEntry(Entry entry) {
        final String name = entry.getEntity().getName();
        final boolean group = entry.getEntity().isGroup();
        final String regionName = entry.getRegion() == null ? null : entry.getRegion().getName();
        final String worldName = entry.getWorld() == null ? null : entry.getWorld().getName();
        final String permission = entry.getPermission();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Locate dependent objects
                PermissionEntity entity = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", name.toLowerCase())
                        .eq("group", group)
                        .findUnique();
                if (entity == null) {
                    databaseInconsistency();
                    return;
                }

                PermissionRegion region = null;
                if (regionName != null) {
                    region = getEbeanServer().find(PermissionRegion.class).where()
                            .eq("name", regionName)
                            .findUnique();
                    if (region == null) {
                        databaseInconsistency();
                        return;
                    }
                }
                
                PermissionWorld world = null;
                if (worldName != null) {
                    world = getEbeanServer().find(PermissionWorld.class).where()
                            .eq("name", worldName)
                            .findUnique();
                    if (world == null) {
                        databaseInconsistency();
                        return;
                    }
                }
                
                Entry dbEntry = getEbeanServer().find(Entry.class).where()
                        .eq("entity", entity)
                        .eq("region", region)
                        .eq("world", world)
                        .eq("permission", permission.toLowerCase())
                        .findUnique();
                if (dbEntry == null) {
                    databaseInconsistency();
                    return;
                }

                getEbeanServer().delete(dbEntry);
            }
        });
    }

    @Override
    public void createOrUpdateMembership(Membership membership) {
        final String name = membership.getGroup().getDisplayName();
        final String member = membership.getMember().toLowerCase();
        final String displayName = membership.getDisplayName();
        final Date expiration = membership.getExpiration();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Locate dependent object
                PermissionEntity group = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", name.toLowerCase())
                        .eq("group", true)
                        .findUnique();
                if (group == null) {
                    group = inconsistentEntity(name, true);
                }

                Membership dbMembership = getEbeanServer().find(Membership.class).where()
                        .eq("group", group)
                        .eq("member", member)
                        .findUnique();
                if (dbMembership == null) {
                    dbMembership = new Membership();
                    dbMembership.setGroup(group);
                    dbMembership.setMember(member);
                    dbMembership.setDisplayName(displayName);
                }
                dbMembership.setExpiration(expiration);
                getEbeanServer().save(dbMembership);
            }
        });
    }

    @Override
    public void deleteEntity(PermissionEntity entity) {
        final String name = entity.getName();
        final boolean group = entity.isGroup();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                PermissionEntity dbEntity = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", name.toLowerCase())
                        .eq("group", group)
                        .findUnique();
                if (dbEntity == null) {
                    databaseInconsistency();
                    return;
                }
                
                if (group) {
                    getEbeanServer().delete(getEbeanServer().find(Inheritance.class).where()
                            .eq("child", dbEntity)
                            .findList());
                    getEbeanServer().delete(getEbeanServer().find(Inheritance.class).where()
                            .eq("parent", dbEntity)
                            .findList());
                    // backwards compat
                    for (PermissionEntity child : getEbeanServer().find(PermissionEntity.class).where()
                            .eq("parent", dbEntity)
                            .eq("group", true)
                            .findList()) {
                        child.setParent(null);
                        getEbeanServer().save(child);
                    }
                }

                getEbeanServer().delete(dbEntity);
            }
        });
    }

    @Override
    public void deleteMembership(Membership membership) {
        final String name = membership.getGroup().getDisplayName();
        final String member = membership.getMember();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Locate dependent object
                PermissionEntity group = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", name.toLowerCase())
                        .eq("group", true)
                        .findUnique();
                if (group == null) {
                    databaseInconsistency();
                    return;
                }

                Membership dbMembership = getEbeanServer().find(Membership.class).where()
                        .eq("group", group)
                        .eq("member", member.toLowerCase())
                        .findUnique();
                if (dbMembership == null) {
                    databaseInconsistency();
                    return;
                }
                
                getEbeanServer().delete(dbMembership);
            }
        });
    }

    @Override
    public void setEntityParent(PermissionEntity entity, PermissionEntity parent) {
        final String name = entity.getDisplayName();
        final String parentName = parent == null ? null : parent.getDisplayName();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                PermissionEntity dbParent = null;
                if (parentName != null) {
                    dbParent = getEbeanServer().find(PermissionEntity.class).where()
                            .eq("name", parentName.toLowerCase())
                            .eq("group", true)
                            .findUnique();
                    if (dbParent == null) {
                        dbParent = inconsistentEntity(parentName, true);
                    }
                }
                
                PermissionEntity dbEntity = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", name.toLowerCase())
                        .eq("group", true)
                        .findUnique();
                if (dbEntity == null) {
                    dbEntity = inconsistentEntity(name, true);
                }
                
                dbEntity.setParent(dbParent);
                getEbeanServer().save(dbEntity);
            }
        });
    }

    @Override
    public void createOrUpdateInheritance(Inheritance inheritance) {
        final String childName = inheritance.getChild().getDisplayName();
        final String parentName = inheritance.getParent().getDisplayName();
        final int ordering = inheritance.getOrdering();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Locate dependent objects
                PermissionEntity child = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", childName.toLowerCase())
                        .eq("group", true)
                        .findUnique();
                if (child == null) {
                    child = inconsistentEntity(childName, true);
                }

                PermissionEntity parent = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", parentName.toLowerCase())
                        .eq("group", true)
                        .findUnique();
                if (parent == null) {
                    parent = inconsistentEntity(parentName, true);
                }
                
                Inheritance dbInheritance = getEbeanServer().find(Inheritance.class).where()
                        .eq("child", child)
                        .eq("parent", parent)
                        .findUnique();
                if (dbInheritance == null) {
                    dbInheritance = new Inheritance();
                    dbInheritance.setChild(child);
                    dbInheritance.setParent(parent);
                }
                dbInheritance.setOrdering(ordering);
                getEbeanServer().save(dbInheritance);
            }
        });
    }

    @Override
    public void deleteInheritance(Inheritance inheritance) {
        final String childName = inheritance.getChild().getDisplayName();
        final String parentName = inheritance.getParent().getDisplayName();
        
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Locate dependent objects
                PermissionEntity child = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", childName.toLowerCase())
                        .eq("group", true)
                        .findUnique();
                if (child == null) {
                    databaseInconsistency();
                    return;
                }

                PermissionEntity parent = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", parentName.toLowerCase())
                        .eq("group", true)
                        .findUnique();
                if (parent == null) {
                    databaseInconsistency();
                    return;
                }
                
                Inheritance dbInheritance = getEbeanServer().find(Inheritance.class).where()
                        .eq("child", child)
                        .eq("parent", parent)
                        .findUnique();
                if (dbInheritance == null) {
                    databaseInconsistency();
                    return;
                }
                
                getEbeanServer().delete(dbInheritance);
            }
        });
    }

    @Override
    public void setEntityPriority(PermissionEntity entity, final int priority) {
        final String name = entity.getDisplayName();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                PermissionEntity dbEntity = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", name.toLowerCase())
                        .eq("group", true)
                        .findUnique();
                if (dbEntity == null) {
                    dbEntity = inconsistentEntity(name, true);
                }

                dbEntity.setPriority(priority);
                getEbeanServer().save(dbEntity);
            }
        });
    }

    @Override
    public void deleteRegions(Collection<PermissionRegion> regions) {
        final Set<String> regionNames = new HashSet<>(regions.size());
        for (PermissionRegion region : regions) {
            regionNames.add(region.getName());
        }

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                boolean inconsistent = false;

                List<PermissionRegion> dbRegions = new ArrayList<>(regionNames.size());
                for (String regionName : regionNames) {
                    PermissionRegion dbRegion = getEbeanServer().find(PermissionRegion.class).where()
                            .eq("name", regionName.toLowerCase())
                            .findUnique();
                    if (dbRegion == null)
                        inconsistent = true;
                    else
                        dbRegions.add(dbRegion);
                }

                if (inconsistent)
                    databaseInconsistency();

                if (!dbRegions.isEmpty())
                    getEbeanServer().delete(dbRegions);
            }
        });
    }

    @Override
    public void deleteWorlds(Collection<PermissionWorld> worlds) {
        final Set<String> worldNames = new HashSet<>(worlds.size());
        for (PermissionWorld world : worlds) {
            worldNames.add(world.getName());
        }

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                boolean inconsistent = false;

                List<PermissionWorld> dbWorlds = new ArrayList<>(worldNames.size());
                for (String worldName : worldNames) {
                    PermissionWorld dbWorld = getEbeanServer().find(PermissionWorld.class).where()
                            .eq("name", worldName.toLowerCase())
                            .findUnique();
                    if (dbWorld == null)
                        inconsistent = true;
                    else
                        dbWorlds.add(dbWorld);
                }

                if (inconsistent)
                    databaseInconsistency();

                if (!dbWorlds.isEmpty())
                    getEbeanServer().delete(dbWorlds);
            }
        });
    }

    @Override
    public void createOrUpdateMetadata(EntityMetadata metadata) {
        final String name = metadata.getEntity().getName();
        final boolean group = metadata.getEntity().isGroup();
        final String metadataName = metadata.getName().toLowerCase();
        final Object value = metadata.getValue();
        
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Locate dependent objects
                PermissionEntity entity = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", name.toLowerCase())
                        .eq("group", group)
                        .findUnique();
                if (entity == null) {
                    entity = inconsistentEntity(name, group);
                }

                EntityMetadata dbMetadata = getEbeanServer().find(EntityMetadata.class).where()
                        .eq("entity", entity)
                        .eq("name", metadataName)
                        .findUnique();
                if (dbMetadata == null) {
                    dbMetadata = new EntityMetadata();
                    dbMetadata.setEntity(entity);
                    dbMetadata.setName(metadataName);
                }

                dbMetadata.setValue(value);
                getEbeanServer().save(dbMetadata);
            }
        });
    }

    @Override
    public void deleteMetadata(EntityMetadata metadata) {
        final String name = metadata.getEntity().getName();
        final boolean group = metadata.getEntity().isGroup();
        final String metadataName = metadata.getName();
        
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Locate dependent objects
                PermissionEntity entity = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", name.toLowerCase())
                        .eq("group", group)
                        .findUnique();
                if (entity == null) {
                    databaseInconsistency();
                    return;
                }

                EntityMetadata dbMetadata = getEbeanServer().find(EntityMetadata.class).where()
                        .eq("entity", entity)
                        .eq("name", metadataName.toLowerCase())
                        .findUnique();
                if (dbMetadata == null) {
                    databaseInconsistency();
                    return;
                }

                getEbeanServer().delete(dbMetadata);
            }
        });
    }

    @Override
    public void updateDisplayName(PermissionEntity entity) {
        final String name = entity.getName();
        final String displayName = entity.getDisplayName();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                PermissionEntity dbEntity = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", name.toLowerCase())
                        .eq("group", false)
                        .findUnique();
                if (dbEntity == null) {
                    dbEntity = inconsistentEntity(name, false);
                }

                dbEntity.setDisplayName(displayName);
                getEbeanServer().save(dbEntity);
            }
        });
    }

    @Override
    public void updateDisplayName(Membership membership) {
        final String name = membership.getGroup().getDisplayName();
        final String member = membership.getMember();
        final String displayName = membership.getDisplayName();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Locate dependent object
                PermissionEntity group = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", name.toLowerCase())
                        .eq("group", true)
                        .findUnique();
                if (group == null) {
                    databaseInconsistency();
                    return;
                }

                Membership dbMembership = getEbeanServer().find(Membership.class).where()
                        .eq("group", group)
                        .eq("member", member.toLowerCase())
                        .findUnique();
                if (dbMembership == null) {
                    databaseInconsistency();
                    return;
                }
                
                dbMembership.setDisplayName(displayName);
                getEbeanServer().save(dbMembership);
            }
        });
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
        
        permissionService.setMemoryState(memoryState);
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

    private void databaseInconsistency() {
        logger.log(Level.WARNING, "Possible database inconsistency detected; please do a /permissions refresh");
    }

    private PermissionEntity inconsistentEntity(String name, boolean group) {
        databaseInconsistency();
        PermissionEntity entity = new PermissionEntity();
        entity.setName(name.toLowerCase());
        entity.setGroup(group);
        entity.setDisplayName(name);
        getEbeanServer().save(entity);
        return entity;
    }

    private PermissionRegion inconsistentRegion(String name) {
        databaseInconsistency();
        PermissionRegion region = new PermissionRegion();
        region.setName(name.toLowerCase());
        getEbeanServer().save(region);
        return region;
    }

    private PermissionWorld inconsistentWorld(String name) {
        databaseInconsistency();
        PermissionWorld world = new PermissionWorld();
        world.setName(name.toLowerCase());
        getEbeanServer().save(world);
        return world;
    }

}
