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
package org.tyrannyofheaven.bukkit.zPermissions.dao;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.tyrannyofheaven.bukkit.zPermissions.WorldPermission;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionRegion;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionWorld;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

public class MemoryPermissionDao implements PermissionDao {

    private Map<String, PermissionRegion> regions = new HashMap<String, PermissionRegion>();
    
    private Map<String, PermissionWorld> worlds = new HashMap<String, PermissionWorld>();
    
    private Map<String, PermissionEntity> players = new HashMap<String, PermissionEntity>();
    
    private Map<String, PermissionEntity> groups = new HashMap<String, PermissionEntity>();
    
    boolean dirty;
    
    public boolean isDirty() {
        return dirty;
    }
    
    public void setDirty() {
        setDirty(true);
    }
    
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public void clearDirty() {
        this.dirty = false;
    }

    private PermissionRegion getRegion(String region, boolean create) {
        PermissionRegion permissionRegion = null;
        if (region != null) {
            region = region.toLowerCase();
            permissionRegion = regions.get(region);
            if (permissionRegion == null) {
                if (create) {
                    permissionRegion = new PermissionRegion();
                    permissionRegion.setName(region);
                    regions.put(region, permissionRegion);
                    setDirty();
                }
                else {
                    throw new IllegalArgumentException("No such region");
                }
            }
        }
        return permissionRegion;
    }

    private PermissionWorld getWorld(String world, boolean create) {
        PermissionWorld permissionWorld = null;
        if (world != null) {
            world = world.toLowerCase();
            permissionWorld = worlds.get(world);
            if (permissionWorld == null) {
                if (create) {
                    permissionWorld = new PermissionWorld();
                    permissionWorld.setName(world);
                    worlds.put(world, permissionWorld);
                    setDirty();
                }
                else {
                    throw new IllegalArgumentException("No such world");
                }
            }
        }
        return permissionWorld;
    }

    private PermissionEntity getEntity(String name, boolean group, boolean create) {
        PermissionEntity entity;
        if (group)
            entity = groups.get(name.toLowerCase());
        else
            entity = players.get(name.toLowerCase());
        if (entity == null && create) {
            entity = new PermissionEntity();
            entity.setName(name.toLowerCase());
            entity.setGroup(group);
            entity.setDisplayName(name);
            if (group)
                groups.put(name.toLowerCase(), entity);
            else
                players.put(name.toLowerCase(), entity);
            setDirty();
        }
        return entity;
    }

    private PermissionEntity getGroup(String name) {
        PermissionEntity group = getEntity(name, true, false);
        if (group == null)
            throw new MissingGroupException(name);
        return group;
    }

    @Override
    public Boolean getPermission(String name, boolean group, String region, String world, String permission) {
        PermissionEntity entity = getEntity(name, group, false);

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

        for (Entry entry : entity.getPermissions()) {
            if (entry.getRegion() == permissionRegion && entry.getWorld() == permissionWorld &&
                    entry.getPermission().equalsIgnoreCase(permission)) {
                return entry.isValue();
            }
        }
        return null;
    }

    @Override
    public void setPermission(String name, boolean group, String region, String world, String permission, boolean value) {
        PermissionEntity owner;
        if (group) {
            owner = getGroup(name);
        }
        else {
            owner = getEntity(name, group, true);
        }

        PermissionRegion permissionRegion = getRegion(region, true);

        PermissionWorld permissionWorld = getWorld(world, true);

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
            
            owner.getPermissions().add(found);
        }

        found.setValue(value);
        setDirty();
    }

    @Override
    public boolean unsetPermission(String name, boolean group, String region, String world, String permission) {
        PermissionEntity entity = getEntity(name, group, false);

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

        for (Iterator<Entry> i = entity.getPermissions().iterator(); i.hasNext();) {
            Entry entry = i.next();
            if (entry.getRegion() == permissionRegion && entry.getWorld() == permissionWorld &&
                    entry.getPermission().equalsIgnoreCase(permission)) {
                i.remove();
                cleanWorldsAndRegions();
                setDirty();
                return true;
            }
        }
        return false;
    }

    @Override
    public void addMember(String groupName, String member) {
        PermissionEntity group = getGroup(groupName);

        for (Membership membership : group.getMemberships()) {
            if (membership.getMember().equalsIgnoreCase(member))
                return;
        }

        Membership membership = new Membership();
        membership.setMember(member.toLowerCase());
        membership.setGroup(group);
        
        group.getMemberships().add(membership);
        setDirty();
    }

    @Override
    public boolean removeMember(String groupName, String member) {
        PermissionEntity group = getGroup(groupName);
        
        for (Iterator<Membership> i = group.getMemberships().iterator(); i.hasNext();) {
            Membership membership = i.next();
            if (membership.getMember().equalsIgnoreCase(member)) {
                i.remove();
                setDirty();
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> getGroups(String member) {
        List<PermissionEntity> result = new ArrayList<PermissionEntity>();
        
        for (PermissionEntity group : groups.values()) {
            for (Membership membership : group.getMemberships()) {
                if (membership.getMember().equalsIgnoreCase(member)) {
                    result.add(group);
                    break;
                }
            }
        }

        Collections.sort(result, new Comparator<PermissionEntity>() {
            @Override
            public int compare(PermissionEntity a, PermissionEntity b) {
                int pri = a.getPriority() - b.getPriority();
                if (pri != 0)
                    return pri;
                return a.getName().compareTo(b.getName());
            }
        });

        List<String> resultString = new ArrayList<String>(result.size());
        for (PermissionEntity group : result) {
            resultString.add(group.getDisplayName());
        }
        return resultString;
    }

    @Override
    public List<String> getMembers(String group) {
        PermissionEntity groupEntity = getEntity(group, true, false);
        if (groupEntity == null)
            return new ArrayList<String>(); // compat with AvajePermissionDao

        List<String> result = new ArrayList<String>();
        for (Membership membership : groupEntity.getMemberships()) {
            result.add(membership.getMember());
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public PermissionEntity getEntity(String name, boolean group) {
        if (group)
            return groups.get(name.toLowerCase());
        else
            return players.get(name.toLowerCase());
    }

    @Override
    public List<PermissionEntity> getEntities(boolean group) {
        if (group)
            return new ArrayList<PermissionEntity>(groups.values());
        else
            return new ArrayList<PermissionEntity>(players.values());
    }

    @Override
    public void setGroup(String playerName, String groupName) {
        PermissionEntity group = getGroup(groupName);

        Membership found = null;
        for (PermissionEntity groupEntity : groups.values()) {
            for (Iterator<Membership> i = groupEntity.getMemberships().iterator(); i.hasNext();) {
                Membership membership = i.next();
                if (membership.getMember().equalsIgnoreCase(playerName)) {
                    if (!membership.getGroup().equals(group)) {
                        i.remove();
                    }
                    else {
                        found = membership;
                    }
                    break;
                }
            }
        }

        if (found == null) {
            found = new Membership();
            found.setMember(playerName.toLowerCase());
            found.setGroup(group);
            
            group.getMemberships().add(found);
        }
        
        setDirty();
    }

    @Override
    public void setParent(String groupName, String parentName) {
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
        }
        else {
            group.setParent(null);
        }
        
        setDirty();
    }

    @Override
    public void setPriority(String groupName, int priority) {
        PermissionEntity group = getGroup(groupName);

        group.setPriority(priority);
        setDirty();
    }

    // Iterate over each world/region, deleting any that are unused
    private void cleanWorldsAndRegions() {
        // Easier to just see what is used
        Set<PermissionRegion> usedRegions = new HashSet<PermissionRegion>();
        Set<PermissionWorld> usedWorlds = new HashSet<PermissionWorld>();
        
        List<PermissionEntity> entities = new ArrayList<PermissionEntity>();
        entities.addAll(groups.values());
        entities.addAll(players.values());
        
        for (PermissionEntity entity : entities) {
            for (Entry entry : entity.getPermissions()) {
                if (entry.getRegion() != null)
                    usedRegions.add(entry.getRegion());
                if (entry.getWorld() != null)
                    usedWorlds.add(entry.getWorld());
            }
        }
        
        // Re-build lists
        regions.clear();
        for (PermissionRegion region : usedRegions) {
            regions.put(region.getName(), region);
        }
        worlds.clear();
        for (PermissionWorld world : usedWorlds) {
            worlds.put(world.getName(), world);
        }
    }

    @Override
    public boolean deleteEntity(String name, boolean group) {
        PermissionEntity entity = getEntity(name, group, false);
        
        if (group) {
            // Deleting a group
            if (entity != null) {
                // Break parent/child relationship
                for (PermissionEntity child : entity.getChildren()) {
                    child.setParent(null);
                }

                // Delete group's entity
                groups.remove(entity.getName().toLowerCase());
                cleanWorldsAndRegions();
                setDirty();
                return true;
            }
        }
        else {
            // Deleting a player

            boolean found = false;

            // Delete memberships
            for (PermissionEntity groupEntity : groups.values()) {
                for (Iterator<Membership> i = groupEntity.getMemberships().iterator(); i.hasNext();) {
                    Membership membership = i.next();
                    if (membership.getMember().equalsIgnoreCase(name)) {
                        i.remove();
                        setDirty();
                        found = true;
                        break;
                    }
                }
            }

            if (entity != null) {
                // Delete player's entity
                players.remove(entity.getName().toLowerCase());
                cleanWorldsAndRegions();
                setDirty();
            }
            
            return found || entity != null;
        }
        
        return false; // nothing to delete
    }

    @Override
    public List<String> getAncestry(String groupName) {
        PermissionEntity group = getEntity(groupName, true, false);
        if (group == null) // NB only time this will be null is if the default group doesn't exist
            return new ArrayList<String>();

        // Build list of group ancestors
        List<String> ancestry = new ArrayList<String>();
        ancestry.add(group.getDisplayName());
        while (group.getParent() != null) {
            group = group.getParent();
            ancestry.add(group.getDisplayName());
        }
        
        // Reverse list (will be applying farthest ancestors first)
        Collections.reverse(ancestry);

        return ancestry;
    }

    @Override
    public List<Entry> getEntries(String name, boolean group) {
        PermissionEntity entity = getEntity(name, group, false);
        if (entity == null) // NB special consideration for non-existent default group
            return Collections.emptyList();

        return new ArrayList<Entry>(entity.getPermissions());
    }

    @Override
    public boolean createGroup(String name) {
        PermissionEntity group = getEntity(name, true, false); // so we know it was created
        if (group == null) {
            group = getEntity(name, true, true);
            return true;
        }
        else
            return false;
    }

    @Override
    public List<String> getEntityNames(boolean group) {
        Collection<PermissionEntity> entities;
        if (group)
            entities = groups.values();
        else
            entities = players.values();
        List<String> result = new ArrayList<String>(entities.size());
        for (PermissionEntity entity : entities) {
            result.add(entity.getDisplayName());
        }
        return result;
    }

    /**
     * Save state of entire system to filesyste.
     * 
     * @param file the file to save to
     * @throws IOException
     */
    public void save(File file) throws IOException {
        if (!isDirty()) return;

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(new SafeConstructor(), new Representer(), options);
        Writer out = new FileWriter(file);
        try {
            yaml.dump(dump(), out);
            clearDirty();
        }
        finally {
            out.close();
        }
    }

    /**
     * Load state of entire system from filesystem.
     * 
     * @param file the file to load from
     * @throws IOException 
     */
    @SuppressWarnings("unchecked")
    public void load(File file) throws IOException {
        Yaml yaml = new Yaml(new SafeConstructor());
        Reader in = new FileReader(file);
        Map<String, Object> input = null;
        try {
            input = (Map<String, Object>)yaml.load(in);
        }
        finally {
            in.close();
        }
        if (input != null) {
            load(input);
            clearDirty();
        }
    }

    // Dump state of entire system to (YAML-friendly) map
    private Map<String, Object> dump() {
        // Players first
        List<Map<String, Object>> players = new ArrayList<Map<String, Object>>();
        for (PermissionEntity player : this.players.values()) {
            Map<String, Object> playerMap = new LinkedHashMap<String, Object>();
            playerMap.put("name", player.getDisplayName());
            playerMap.put("permissions", dumpPermissions(player));
            players.add(playerMap);
        }

        // Groups next
        List<Map<String, Object>> groups = new ArrayList<Map<String, Object>>();
        for (PermissionEntity group : this.groups.values()) {
            Map<String, Object> groupMap = new LinkedHashMap<String, Object>();
            groupMap.put("name", group.getName());
            groupMap.put("permissions", (Object)dumpPermissions(group));
            groupMap.put("priority", group.getPriority());
            if (group.getParent() != null)
                groupMap.put("parent", group.getParent().getDisplayName());
            List<String> members = new ArrayList<String>(group.getMemberships().size());
            for (Membership membership : group.getMemberships()) {
                members.add(membership.getMember());
            }
            groupMap.put("members", members);
            
            groups.add(groupMap);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("players", players);
        result.put("groups", groups);
        return result;
    }

    // Load state of entire system from (YAML-friendly) map
    @SuppressWarnings("unchecked")
    private void load(Map<String, Object> input) {
        regions.clear();
        worlds.clear();
        players.clear();
        groups.clear();

        for (Map<String, Object> playerMap : (List<Map<String, Object>>)input.get("players")) {
            String name = (String)playerMap.get("name");
            Map<String, Boolean> permissions = (Map<String, Boolean>)playerMap.get("permissions");
            PermissionEntity player = getEntity(name, false, true);
            loadPermissions(permissions, player);
        }
        
        for (Map<String, Object> groupMap : (List<Map<String, Object>>)input.get("groups")) {
            String name = (String)groupMap.get("name");
            Map<String, Boolean> permissions = (Map<String, Boolean>)groupMap.get("permissions");
            Number priority = (Number)groupMap.get("priority");
            String parent = (String)groupMap.get("parent");
            List<String> members = (List<String>)groupMap.get("members");
            
            PermissionEntity group = getEntity(name, true, true);
            loadPermissions(permissions, group);
            group.setPriority(priority.intValue());
            if (parent != null)
                group.setParent(getEntity(parent, true, true));
            for (String member : members) {
                Membership membership = new Membership();
                membership.setMember(member);
                membership.setGroup(group);
                group.getMemberships().add(membership);
            }
        }
    }

    // Create a map that describes permissions for a PermissionEntity
    private Map<String, Boolean> dumpPermissions(PermissionEntity entity) {
        Map<String, Boolean> result = new HashMap<String, Boolean>();
        for (Entry e : entity.getPermissions()) {
            WorldPermission wp = new WorldPermission(e.getRegion() == null ? null : e.getRegion().getName(),
                    e.getRegion() == null ? null : e.getRegion().getName(), e.getPermission());
            result.put(wp.toString(), e.isValue());
        }
        return result;
    }

    // Load permissions for a PermissionEntity from a map
    private void loadPermissions(Map<String, Boolean> input, PermissionEntity entity) {
        for (Map.Entry<String, Boolean> me : input.entrySet()) {
            Entry entry = new Entry();

            WorldPermission wp = new WorldPermission(me.getKey());
            entry.setRegion(wp.getRegion() == null ? null : getRegion(wp.getRegion(), true));
            entry.setWorld(wp.getWorld() == null ? null : getWorld(wp.getWorld(), true));
            entry.setPermission(wp.getPermission());
            entry.setValue(me.getValue());

            entry.setEntity(entity);
            entity.getPermissions().add(entry);
        }
    }

}
