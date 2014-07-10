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

import static org.tyrannyofheaven.bukkit.util.uuid.UuidUtils.uncanonicalizeUuid;
import static org.tyrannyofheaven.bukkit.zPermissions.dao.InMemoryPermissionService.getEntity;
import static org.tyrannyofheaven.bukkit.zPermissions.dao.InMemoryPermissionService.getRegion;
import static org.tyrannyofheaven.bukkit.zPermissions.dao.InMemoryPermissionService.getWorld;
import static org.tyrannyofheaven.bukkit.zPermissions.dao.InMemoryPermissionService.rememberMembership;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.tyrannyofheaven.bukkit.zPermissions.QualifiedPermission;
import org.tyrannyofheaven.bukkit.zPermissions.dao.InMemoryPermissionService.MemoryState;
import org.tyrannyofheaven.bukkit.zPermissions.model.EntityMetadata;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Inheritance;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionRegion;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionWorld;
import org.tyrannyofheaven.bukkit.zPermissions.util.Utils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Flat-file based PermissionDao implementation.
 * 
 * @author zerothangel
 */
public class FilePermissionDao implements PermissionDao {

    private Logger logger = Logger.getLogger(getClass().getName());

    private final InMemoryPermissionService permissionService;

    private boolean dirty;

    public FilePermissionDao(InMemoryPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public synchronized boolean isDirty() {
        return dirty;
    }

    private void setDirty() {
        setDirty(true);
    }

    private synchronized void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    private synchronized void clearDirty() {
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

    /**
     * Save state of entire system to filesyste.
     * 
     * @param file the file to save to
     * @throws IOException
     */
    public void save(File file) throws IOException {
        if (!isDirty()) return;

        Map<String, Object> dump;
        synchronized (permissionService) {
            dump = dump();
        }

        File newFile = new File(file.getParentFile(), file.getName() + ".new");

        // Write out file
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(new SafeConstructor(), new Representer(), options);
        Writer out = new FileWriter(newFile);
        try {
            out.write("# DO NOT EDIT -- file is written to periodically!\n" +
                    "# Seriously, do not edit. Today it is YAML, tomorrow it may not be.\n" +
                    "# If you edit this file and you have problems, you are on your own!\n");
            yaml.dump(dump, out);
        }
        finally {
            out.close();
        }

        File backupFile = new File(file.getParentFile(), file.getName() + "~");

        // Delete old backup (might be necessary on some platforms)
        if (backupFile.exists() && !backupFile.delete()) {
            logger.log(Level.WARNING, "Error deleting configuration " + backupFile);
            // Continue despite failure
        }

        // Back up old config
        if (file.exists() && !file.renameTo(backupFile)) {
            logger.log(Level.SEVERE, String.format("Error renaming %s to %s", file, backupFile));
            return; // no backup, abort
        }

        // Rename new file to config
        if (!newFile.renameTo(file)) {
            logger.log(Level.SEVERE, String.format("Error renaming %s to %s", newFile, file));
            return;
        }

        clearDirty();
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
        List<Map<String, Object>> players = new ArrayList<>();
        for (PermissionEntity player : Utils.sortPlayers(permissionService.getPlayers().values())) {
            Map<String, Object> playerMap = new LinkedHashMap<>();
            playerMap.put("uuid", player.getName());
            playerMap.put("name", player.getDisplayName());
            playerMap.put("permissions", dumpPermissions(player));
            playerMap.put("metadata", dumpMetadata(player));
            players.add(playerMap);
        }

        // Groups next
        List<Map<String, Object>> groups = new ArrayList<>();
        for (PermissionEntity group : Utils.sortGroups(permissionService.getGroups().values())) {
            Map<String, Object> groupMap = new LinkedHashMap<>();
            groupMap.put("name", group.getDisplayName());
            groupMap.put("permissions", dumpPermissions(group));
            groupMap.put("metadata", dumpMetadata(group));
            groupMap.put("priority", group.getPriority());
            List<PermissionEntity> parents = group.getParents();
            if (!parents.isEmpty()) {
                List<String> parentNames = new ArrayList<>(parents.size());
                for (PermissionEntity parent : parents)
                    parentNames.add(parent.getDisplayName());
                groupMap.put("parents", parentNames);
            }
            // Permanent members
            List<Map<String, Object>> members = new ArrayList<>();
            List<Map<String, Object>> tempMembers = new ArrayList<>();
            for (Membership membership : Utils.sortMemberships(group.getMemberships())) {
                Map<String, Object> tempMemberMap = new LinkedHashMap<>();
                tempMemberMap.put("uuid", membership.getMember());
                tempMemberMap.put("name", membership.getDisplayName());
                if (membership.getExpiration() == null) {
                    members.add(tempMemberMap);
                }
                else {
                    tempMemberMap.put("uuid", membership.getMember());
                    tempMemberMap.put("name", membership.getDisplayName());
                    tempMemberMap.put("expiration", membership.getExpiration());

                    tempMembers.add(tempMemberMap);
                }
            }
            groupMap.put("members", members);
            groupMap.put("tempmembers", tempMembers);
            
            groups.add(groupMap);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("players", players);
        result.put("groups", groups);
        return result;
    }

    // Load state of entire system from (YAML-friendly) map
    @SuppressWarnings("unchecked")
    private void load(Map<String, Object> input) {
        MemoryState memoryState = new MemoryState();

        for (Map<String, Object> playerMap : (List<Map<String, Object>>)input.get("players")) {
            UUID uuid = uncanonicalizeUuid((String)playerMap.get("uuid"));
            String name = (String)playerMap.get("name");
            Map<String, Boolean> permissions = (Map<String, Boolean>)playerMap.get("permissions");
            PermissionEntity player = getEntity(memoryState, name, uuid, false);
            loadPermissions(memoryState, permissions, player);
            Map<String, Object> metadata = (Map<String, Object>)playerMap.get("metadata");
            if (metadata == null) // backwards compat
                metadata = Collections.emptyMap();
            loadMetadata(metadata, player);
        }
        
        for (Map<String, Object> groupMap : (List<Map<String, Object>>)input.get("groups")) {
            String name = (String)groupMap.get("name");
            Map<String, Boolean> permissions = (Map<String, Boolean>)groupMap.get("permissions");
            Number priority = (Number)groupMap.get("priority");
            String parent = (String)groupMap.get("parent");
            List<String> parents = (List<String>)groupMap.get("parents");
            List<Map<String, Object>> members = (List<Map<String, Object>>)groupMap.get("members");
            List<Map<String, Object>> tempMembers = (List<Map<String, Object>>)groupMap.get("tempmembers");
            if (tempMembers == null) // backwards compat
                tempMembers = Collections.emptyList();
            Map<String, Object> metadata = (Map<String, Object>)groupMap.get("metadata");
            if (metadata == null) // backwards compat
                metadata = Collections.emptyMap();

            PermissionEntity group = getEntity(memoryState, name, null, true);
            loadPermissions(memoryState, permissions, group);
            loadMetadata(metadata, group);
            group.setPriority(priority.intValue());
            if (parent != null) {
                // Backwards compatibility
                PermissionEntity parentEntity = getEntity(memoryState, parent, null, true);

                Inheritance i = new Inheritance();
                i.setChild(group);
                i.setParent(parentEntity);
                i.setOrdering(0);

                // Add to maps
                group.getInheritancesAsChild().add(i);
                parentEntity.getInheritancesAsParent().add(i);
            }
            else if (parents != null) {
                int order = 0;
                for (String p : parents) {
                    PermissionEntity parentEntity = getEntity(memoryState, p, null, true);
                    
                    Inheritance i = new Inheritance();
                    i.setChild(group);
                    i.setParent(parentEntity);
                    i.setOrdering(order);
                    order += 100;
                    
                    // Add to maps
                    group.getInheritancesAsChild().add(i);
                    parentEntity.getInheritancesAsParent().add(i);
                }
            }
            for (Map<String, Object> memberMap : members) {
                Membership membership = new Membership();
                membership.setMember(((String)memberMap.get("uuid")).toLowerCase());
                membership.setDisplayName(((String)memberMap.get("name")));
                membership.setGroup(group);
                group.getMemberships().add(membership);
                
                rememberMembership(memoryState, membership);
            }
            for (Map<String, Object> tempMemberMap : tempMembers) {
                Membership membership = new Membership();
                membership.setMember(((String)tempMemberMap.get("uuid")).toLowerCase());
                membership.setDisplayName(((String)tempMemberMap.get("name")));
                membership.setGroup(group);
                membership.setExpiration((Date)tempMemberMap.get("expiration"));
                group.getMemberships().add(membership);
                
                rememberMembership(memoryState, membership);
            }
        }
        
        permissionService.setMemoryState(memoryState);
    }

    // Create a map that describes permissions for a PermissionEntity
    private Map<String, Boolean> dumpPermissions(PermissionEntity entity) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (Entry e : Utils.sortPermissions(entity.getPermissions())) {
            QualifiedPermission wp = new QualifiedPermission(e.getRegion() == null ? null : e.getRegion().getName(),
                    e.getWorld() == null ? null : e.getWorld().getName(), e.getPermission());
            result.put(wp.toString(), e.isValue());
        }
        return result;
    }

    // Load permissions for a PermissionEntity from a map
    private void loadPermissions(MemoryState memoryState, Map<String, Boolean> input, PermissionEntity entity) {
        for (Map.Entry<String, Boolean> me : input.entrySet()) {
            Entry entry = new Entry();

            QualifiedPermission wp = new QualifiedPermission(me.getKey());
            entry.setRegion(wp.getRegion() == null ? null : getRegion(memoryState, wp.getRegion()));
            entry.setWorld(wp.getWorld() == null ? null : getWorld(memoryState, wp.getWorld()));
            entry.setPermission(wp.getPermission().toLowerCase());
            entry.setValue(me.getValue());

            entry.setEntity(entity);
            entity.getPermissions().add(entry);
        }
    }

    private Map<String, Object> dumpMetadata(PermissionEntity entity) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (EntityMetadata em : Utils.sortMetadata(entity.getMetadata())) {
            result.put(em.getName(), em.getValue());
        }
        return result;
    }

    private void loadMetadata(Map<String, Object> input, PermissionEntity entity) {
        for (Map.Entry<String, Object> me : input.entrySet()) {
            try {
                EntityMetadata em = new EntityMetadata();
                em.setName(me.getKey().toLowerCase());
                em.setValue(me.getValue());
                em.setEntity(entity);
                entity.getMetadata().add(em);
            }
            catch (IllegalArgumentException e) {
                // Ignore invalid value
            }
        }
        
        entity.updateMetadataMap();
    }

}
