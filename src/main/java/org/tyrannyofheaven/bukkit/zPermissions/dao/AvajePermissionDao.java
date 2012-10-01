/*
 * Copyright 2011 ZerothAngel <zerothangel@tyrannyofheaven.org>
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionRegion;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionWorld;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.cache.ServerCache;

/**
 * DAO implementation using Avaje Ebeans.
 * 
 * @author zerothangel
 */
public class AvajePermissionDao implements PermissionDao {

    private final EbeanServer ebean;
    
    public AvajePermissionDao(EbeanServer ebean) {
        this.ebean = ebean;
    }

    // Retrieve associated EbeanServer
    private EbeanServer getEbeanServer() {
        return ebean;
    }

    private void clearQueryCache(Class<?> clazz) {
        ServerCache cache = getEbeanServer().getServerCacheManager().getQueryCache(clazz);
        cache.clear();
    }

    // Retrieve named region, optionally creating it
    // If region is null, null is returned denoting a world-wide entry.
    private PermissionRegion getRegion(String region, boolean create) {
        PermissionRegion permissionRegion = null;
        if (region != null) {
            permissionRegion = getEbeanServer().find(PermissionRegion.class).where()
                .eq("name", region.toLowerCase())
                .findUnique();
            if (permissionRegion == null) {
                if (create) {
                    permissionRegion = new PermissionRegion();
                    permissionRegion.setName(region.toLowerCase());
                }
                else {
                    throw new IllegalArgumentException("No such region");
                }
            }
        }
        return permissionRegion;
    }

    // Retrieve named world, optionally creating it
    // If world is null, null is returned denoting a global entry.
    private PermissionWorld getWorld(String world, boolean create) {
        PermissionWorld permissionWorld = null;
        if (world != null) {
            permissionWorld = getEbeanServer().find(PermissionWorld.class).where()
                .eq("name", world.toLowerCase())
                .findUnique();
            if (permissionWorld == null) {
                if (create) {
                    permissionWorld = new PermissionWorld();
                    permissionWorld.setName(world.toLowerCase());
                }
                else {
                    throw new IllegalArgumentException("No such world");
                }
            }
        }
        return permissionWorld;
    }

    // Retrieve the named entity/type, optionally creating it.
    private PermissionEntity getEntity(String name, boolean group, boolean create) {
        PermissionEntity entity = getEbeanServer().find(PermissionEntity.class).where()
            .eq("name", name.toLowerCase())
            .eq("group", group)
            .findUnique();
        if (entity == null && create) {
            entity = new PermissionEntity();
            entity.setName(name.toLowerCase());
            entity.setGroup(group);
            entity.setDisplayName(name);
        }
        return entity;
    }

    // Retrieve the named group, throw exception if it doesn't exist
    private PermissionEntity getGroup(String name) {
        PermissionEntity group = getEntity(name, true, false);
        if (group == null)
            throw new MissingGroupException(name); // have a bad feeling about this being unchecked
        return group;
    }
    
    // Ensures an explicit transaction is open. Used for DAO methods that perform
    // multiple find/save operations.
    private void checkTransaction() {
        if (getEbeanServer().currentTransaction() == null)
            throw new IllegalStateException("Needs a transaction");
    }

    @Override
    public Boolean getPermission(String name, boolean group, String region, String world, String permission) {
        checkTransaction();

        PermissionRegion permissionRegion;
        try {
            permissionRegion = getRegion(region, false);
        }
        catch (IllegalArgumentException e) {
            return null;
        }

        PermissionWorld permissionWorld;
        try {
            permissionWorld = getWorld(world, false);
        }
        catch (IllegalArgumentException e) {
            return null;
        }

        Entry entry = getEbeanServer().find(Entry.class).where()
            .eq("entity.name", name.toLowerCase())
            .eq("entity.group", group)
            .eq("region", permissionRegion)
            .eq("world", permissionWorld)
            .eq("permission", permission.toLowerCase())
            .findUnique();
        if (entry != null)
            return entry.isValue();
        return null;
    }

    @Override
    public void setPermission(String name, boolean group, String region, String world, String permission, boolean value) {
        checkTransaction();

        PermissionEntity owner;
        if (group) {
            owner = getGroup(name);
        }
        else {
            owner = getEntity(name, group, false); // so we know it was created
            if (owner == null) {
                owner = getEntity(name, group, true);
                getEbeanServer().save(owner);
                clearQueryCache(PermissionEntity.class);
            }
        }

        PermissionRegion permissionRegion = getRegion(region, true);
        if (permissionRegion != null)
            getEbeanServer().save(permissionRegion);

        PermissionWorld permissionWorld = getWorld(world, true);
        if (permissionWorld != null)
            getEbeanServer().save(permissionWorld);

        permission = permission.toLowerCase();

        Entry found = null;
        for (Entry entry : owner.getPermissions()) {
            if (permission.equals(entry.getPermission()) &&
                    (permissionRegion == null ? entry.getRegion() == null : permissionRegion.equals(entry.getRegion())) &&
                    (permissionWorld == null ? entry.getWorld() == null : permissionWorld.equals(entry.getWorld()))) {
                found = entry;
                break;
            }
        }

        if (found == null) {
            found = new Entry();
            found.setEntity(owner);
            found.setRegion(permissionRegion);
            found.setWorld(permissionWorld);
            found.setPermission(permission);
        }

        found.setValue(value);

        getEbeanServer().save(found);
        clearQueryCache(Entry.class);
    }

    @Override
    public boolean unsetPermission(String name, boolean group, String region, String world, String permission) {
        checkTransaction();

        PermissionRegion permissionRegion;
        try {
            permissionRegion = getRegion(region, false);
        }
        catch (IllegalArgumentException e) {
            return false;
        }

        PermissionWorld permissionWorld;
        try {
            permissionWorld = getWorld(world, false);
        }
        catch (IllegalArgumentException e) {
            return false;
        }

        Entry entry = getEbeanServer().find(Entry.class).where()
            .eq("entity.name", name.toLowerCase())
            .eq("entity.group", group)
            .eq("region", permissionRegion)
            .eq("world", permissionWorld)
            .eq("permission", permission.toLowerCase())
            .findUnique();

        if (entry != null) {
            getEbeanServer().delete(entry);
            cleanWorldsAndRegions();
            clearQueryCache(Entry.class);
            return true;
        }
        
        return false;
    }

    @Override
    public void addMember(String groupName, String member) {
        checkTransaction();

        PermissionEntity group = getGroup(groupName);

        Membership membership = getEbeanServer().find(Membership.class).where()
            .eq("member", member.toLowerCase())
            .eq("group", group)
            .findUnique();

        if (membership == null) {
            membership = new Membership();
            membership.setMember(member.toLowerCase());
            membership.setGroup(group);
            getEbeanServer().save(membership);
            clearQueryCache(Membership.class);
        }
    }

    @Override
    public boolean removeMember(String groupName, String member) {
        checkTransaction();

        // Should consider getGroup() instead. But this method already returns false on non-existent group.
        PermissionEntity group = getEntity(groupName, true, false);

        if (group != null) {
            Membership membership = getEbeanServer().find(Membership.class).where()
                .eq("member", member.toLowerCase())
                .eq("group", group)
                .findUnique();

            if (membership != null) {
                getEbeanServer().delete(membership);
                clearQueryCache(Membership.class);
                return true;
            }
        }
        
        return false;
    }

    @Override
    public List<String> getGroups(String member) {
        // NB: No explicit transaction required
        List<Membership> memberships = getEbeanServer().createQuery(Membership.class, "find Membership fetch group (priority, name, displayName) where member = :member order by group.priority, group.name")
            .setParameter("member", member.toLowerCase())
            .setUseQueryCache(true)
            .findList();

        List<String> groups = new ArrayList<String>();
        for (Membership membership : memberships) {
            groups.add(membership.getGroup().getDisplayName());
        }
        return groups;
    }

    @Override
    public PermissionEntity getEntity(String name, boolean group) {
        // NB: No explicit transaction required
        return getEbeanServer().find(PermissionEntity.class).where()
            .eq("name", name.toLowerCase())
            .eq("group", group)
            .findUnique();
    }

    @Override
    public void setGroup(String playerName, String groupName) {
        checkTransaction();

        PermissionEntity group = getGroup(groupName);

        List<Membership> memberships = getEbeanServer().find(Membership.class).where()
            .eq("member", playerName.toLowerCase())
            .findList();

        Membership found = null;
        List<Membership> toDelete = new ArrayList<Membership>();
        for (Membership membership : memberships) {
            if (!membership.getGroup().equals(group)) {
                toDelete.add(membership);
            }
            else {
                found = membership;
            }
        }
        getEbeanServer().delete(toDelete);

        if (found == null) {
            found = new Membership();
            found.setMember(playerName.toLowerCase());
            found.setGroup(group);
            getEbeanServer().save(found);
        }
        clearQueryCache(Membership.class);
    }

    @Override
    public List<PermissionEntity> getEntities(boolean group) {
        // NB: No explicit transaction required
        return getEbeanServer().find(PermissionEntity.class).where()
            .eq("group", group)
            .findList();
    }

    @Override
    public void setParent(String groupName, String parentName) {
        checkTransaction();

        PermissionEntity group = getGroup(groupName);

        if (parentName != null) {
            PermissionEntity parent = getGroup(parentName);

            // Check for a cycle
            PermissionEntity check = parent;
            while (check != null) {
                if (group.equals(check)) {
                    throw new DaoException("This would result in an inheritance cycle!");
                }
                check = check.getParent();
            }

            group.setParent(parent);
            parent.getChildren().add(group);
            getEbeanServer().save(parent);
        }
        else {
            group.setParent(null);
        }
        getEbeanServer().save(group);
        clearQueryCache(PermissionEntity.class);
    }

    @Override
    public void setPriority(String groupName, int priority) {
        checkTransaction();

        PermissionEntity group = getGroup(groupName);

        group.setPriority(priority);

        getEbeanServer().save(group);
        clearQueryCache(Membership.class);
    }

    // Iterate over each world/region, deleting any that are unused
    private void cleanWorldsAndRegions() {
        for (PermissionRegion region : getEbeanServer().find(PermissionRegion.class).findList()) {
            if (getEbeanServer().createQuery(Entry.class).where().eq("region", region).findRowCount() == 0) {
                // No more entries reference this region
                getEbeanServer().delete(region);
            }
        }
        for (PermissionWorld world : getEbeanServer().find(PermissionWorld.class).findList()) {
            if (getEbeanServer().createQuery(Entry.class).where().eq("world", world).findRowCount() == 0) {
                // No more entries reference this world
                getEbeanServer().delete(world);
            }
        }
    }

    @Override
    public boolean deleteEntity(String name, boolean group) {
        checkTransaction();

        PermissionEntity entity = getEntity(name, group, false);
        
        if (group) {
            // Deleting a group
            if (entity != null) {
                // Break parent/child relationship
                for (PermissionEntity child : entity.getChildren()) {
                    child.setParent(null);
                    getEbeanServer().save(child);
                }

                // Delete group's entity
                getEbeanServer().delete(entity); // should cascade to entries and memberships
                cleanWorldsAndRegions();
                clearQueryCache(Membership.class);
                clearQueryCache(PermissionEntity.class);
                clearQueryCache(Entry.class);
                return true;
            }
        }
        else {
            // Deleting a player

            // Delete memberships
            List<Membership> memberships = getEbeanServer().find(Membership.class).where()
                .eq("member", name.toLowerCase())
                .findList();
            getEbeanServer().delete(memberships);
            clearQueryCache(Membership.class);

            if (entity != null) {
                // Delete player's entity
                getEbeanServer().delete(entity); // should cascade to entries
                cleanWorldsAndRegions();
                clearQueryCache(PermissionEntity.class);
                clearQueryCache(Entry.class);
            }
            
            return !memberships.isEmpty() || entity != null;
        }
        
        return false; // nothing to delete
    }

    @Override
    public List<String> getMembers(String group) {
        // No explicit transaction required
        List<Membership> memberships = getEbeanServer().createQuery(Membership.class, "find Membership where group.name = :groupName order by member")
                .setParameter("groupName", group.toLowerCase())
                .setUseQueryCache(true)
                .findList();
        
        List<String> result = new ArrayList<String>(memberships.size());
        for (Membership membership : memberships) {
            result.add(membership.getMember());
        }
        return result;
    }

    @Override
    public List<String> getAncestry(String groupName) {
        checkTransaction();

        PermissionEntity group = getEbeanServer().createQuery(PermissionEntity.class, "find PermissionEntity where group = true and name = :groupName")
                .setParameter("groupName", groupName.toLowerCase())
                .setUseQueryCache(true)
                .findUnique();
        if (group == null) // NB only time this will be null is if the default group doesn't exist
            return new ArrayList<String>();

        // Build list of group ancestors
        List<String> ancestry = new ArrayList<String>();
        ancestry.add(group.getDisplayName());
        while (group.getParent() != null) {
            // Very very strange happenings with Avaje force me to do this...
            group = getEbeanServer().createQuery(PermissionEntity.class, "find PermissionEntity where group = true and id = :groupId")
                    .setParameter("groupId", getEbeanServer().getBeanId(group.getParent()))
                    .setUseQueryCache(true)
                    .findUnique();
            ancestry.add(group.getDisplayName());
        }
        
        // Reverse list (will be applying farthest ancestors first)
        Collections.reverse(ancestry);

        return ancestry;
    }

    @Override
    public List<Entry> getEntries(String name, boolean group) {
        checkTransaction();
        
        return getEbeanServer().createQuery(Entry.class, "find Entry where entity.name = :name and entity.group = :group")
            .setParameter("name", name.toLowerCase())
            .setParameter("group", group)
            .setUseQueryCache(true)
            .findList();
    }

    @Override
    public boolean createGroup(String name) {
        checkTransaction();

        PermissionEntity group = getEntity(name, true, false); // so we know it was created
        if (group == null) {
            group = getEntity(name, true, true);
            getEbeanServer().save(group);
            clearQueryCache(PermissionEntity.class);
            return true;
        }
        else
            return false;
    }

    @Override
    public List<String> getEntityNames(boolean group) {
        List<PermissionEntity> entities = getEbeanServer().createQuery(PermissionEntity.class, "find PermissionEntity where group = :group")
                .setParameter("group", group)
                .setUseQueryCache(true)
                .findList();
        List<String> result = new ArrayList<String>(entities.size());
        for (PermissionEntity entity : entities) {
            result.add(entity.getDisplayName());
        }
        return result;
    }

}
