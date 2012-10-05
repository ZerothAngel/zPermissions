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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

/**
 * Flat-file based PermissionDao implementation.
 * 
 * @author asaddi
 */
public class MemoryPermissionDao extends BaseMemoryPermissionDao {

    private boolean dirty;

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
        for (PermissionEntity player : getPlayers().values()) {
            Map<String, Object> playerMap = new LinkedHashMap<String, Object>();
            playerMap.put("name", player.getDisplayName());
            playerMap.put("permissions", dumpPermissions(player));
            players.add(playerMap);
        }

        // Groups next
        List<Map<String, Object>> groups = new ArrayList<Map<String, Object>>();
        for (PermissionEntity group : getGroups().values()) {
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
        getRegions().clear();
        getWorlds().clear();
        getPlayers().clear();
        getGroups().clear();
        getReverseMembershipMap().clear();

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
            if (parent != null) {
                PermissionEntity parentEntity = getEntity(parent, true, true);
                group.setParent(parentEntity);
                parentEntity.getChildren().add(group);
            }
            for (String member : members) {
                Membership membership = new Membership();
                membership.setMember(member.toLowerCase());
                membership.setGroup(group);
                group.getMemberships().add(membership);
                
                rememberMembership(group, membership);
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

    @Override
    protected void createRegion(PermissionRegion region) {
        setDirty();
    }

    @Override
    protected void createWorld(PermissionWorld world) {
        setDirty();
    }

    @Override
    protected void createEntity(PermissionEntity entity) {
        setDirty();
    }

    @Override
    protected void createOrUpdateEntry(Entry entry) {
        setDirty();
    }

    @Override
    protected void deleteEntry(Entry entry) {
        setDirty();
    }

    @Override
    protected void createMembership(Membership membership) {
        setDirty();
    }

    @Override
    protected void deleteEntity(PermissionEntity entity) {
        setDirty();
    }

    @Override
    protected void deleteMembership(Membership membership) {
        setDirty();
    }

    @Override
    protected void setEntityParent(PermissionEntity entity, PermissionEntity parent) {
        setDirty();
    }

    @Override
    protected void setEntityPriority(PermissionEntity entity, int priority) {
        setDirty();
    }

    @Override
    protected void deleteRegions(Collection<PermissionRegion> regions) {
        setDirty();
    }

    @Override
    protected void deleteWorlds(Collection<PermissionWorld> worlds) {
        setDirty();
    }

}
