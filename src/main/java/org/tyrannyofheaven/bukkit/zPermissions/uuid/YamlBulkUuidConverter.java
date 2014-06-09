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
package org.tyrannyofheaven.bukkit.zPermissions.uuid;

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.log;
import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.warn;
import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.hasText;
import static org.tyrannyofheaven.bukkit.util.uuid.UuidUtils.canonicalizeUuid;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.util.uuid.UuidDisplayName;
import org.tyrannyofheaven.bukkit.util.uuid.UuidResolver;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

public class YamlBulkUuidConverter implements BulkUuidConverter {

    private final Plugin plugin;

    private final UuidResolver uuidResolver;

    private final File dataFile;

    public YamlBulkUuidConverter(Plugin plugin, UuidResolver uuidResolver, File dataFile) {
        this.plugin = plugin;
        this.uuidResolver = uuidResolver;
        this.dataFile = dataFile;
    }

    public UuidResolver getUuidResolver() {
        return uuidResolver;
    }

    @Override
    public void migrate() throws Exception {
        // Does it exist?
        if (!dataFile.exists()) return;

        // Read it in
        Yaml yaml = new Yaml(new SafeConstructor());
        Reader in = new FileReader(dataFile);
        Map<String, Object> data = null;
        try {
            data = (Map<String, Object>)yaml.load(in);
        }
        finally {
            in.close();
        }
        
        // Gather usernames
        Set<String> usernames = new HashSet<>();
        int players = preparePlayers(data, usernames);
        log(plugin, "%d player%s to migrate", players, players == 1 ? "" : "s");
        List<Map<String, Object>> groups = prepareGroups(data, usernames);
        log(plugin, "%d group%s to migrate", groups.size(), groups.size() == 1 ? "" : "s");

        if (players == 0 && groups.size() == 0) {
            log(plugin, "Nothing to migrate");
            return;
        }

        // Perform lookup
        log(plugin, "Looking up %d UUID%s...", usernames.size(), usernames.size() == 1 ? "" : "s");
        Map<String, UuidDisplayName> resolved = getUuidResolver().resolve(usernames);

        // Migrate
        log(plugin, "Migrating players...");
        migratePlayers(data, resolved);
        log(plugin, "Migrating groups...");
        migrateGroups(groups, resolved);

        log(plugin, "Saving...");
        // Save converted version
        File newFile = new File(dataFile.getParentFile(), dataFile.getName() + ".new");

        // Write out file
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yaml = new Yaml(new SafeConstructor(), new Representer(), options);
        Writer out = new FileWriter(newFile);
        try {
            out.write("# DO NOT EDIT -- file is written to periodically!\n");
            yaml.dump(data, out);
        }
        finally {
            out.close();
        }

        File backupFile = new File(dataFile.getParentFile(), dataFile.getName() + "~");

        // Delete old backup (might be necessary on some platforms)
        if (backupFile.exists() && !backupFile.delete()) {
            warn(plugin, "Error deleting configuration %s", backupFile);
            // Continue despite failure
        }

        // Back up old config
        if (dataFile.exists() && !dataFile.renameTo(backupFile)) {
            warn(plugin, "Error renaming %s to %s", dataFile, backupFile);
            return; // no backup, abort
        }

        // Rename new file to config
        if (!newFile.renameTo(dataFile)) {
            warn(plugin, "Error renaming %s to %s", newFile, dataFile);
        }

        log(plugin, "Migration done");
    }

    private int preparePlayers(Map<String, Object> data, Set<String> usernames) {
        int count = 0;
        for (Map<String, Object> player : (List<Map<String, Object>>)data.get("players")) {
            String name = (String)player.get("name");
            if (!hasText(name))
                throw new IllegalStateException("name must have a value");
            String uuidString = (String)player.get("uuid");
            if (uuidString == null) {
                // Hasn't been migrated yet
                count++;
                usernames.add(name.toLowerCase());
            }
        }
        return count;
    }

    private List<Map<String, Object>> prepareGroups(Map<String, Object> data, Set<String> usernames) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> group : (List<Map<String, Object>>)data.get("groups")) {
            boolean migrate = false;
            List<?> members = (List<?>)group.get("members");
            // pre-UUID -> list of strings
            // post-UUID -> list of maps
            if (!members.isEmpty()) {
                // Grab first entry
                Object first = members.get(0);
                if (first instanceof String) {
                    // Needs migration
                    migrate = true;
                    for (Object username : members) {
                        usernames.add(((String)username).toLowerCase());
                    }
                }
            }

            // Next check temp members
            List<Map<String, Object>> tempMembers = (List<Map<String, Object>>)group.get("tempmembers");
            if (tempMembers == null) // backwards compat
                tempMembers = Collections.emptyList();
            for (Map<String, Object> tempMember : tempMembers) {
                String name = (String)tempMember.get("member");
                if (name == null) break; // Already migrated
                if (!hasText(name))
                    throw new IllegalStateException("member must have a value");
                if (!tempMember.containsKey("uuid")) {
                    migrate = true;
                    usernames.add(name.toLowerCase());
                }
            }

            if (migrate) result.add(group);
        }

        return result;
    }

    private void migratePlayers(Map<String, Object> data, Map<String, UuidDisplayName> resolved) {
        for (Iterator<Map<String, Object>> i = ((List<Map<String, Object>>)data.get("players")).iterator(); i.hasNext();) {
            Map<String, Object> player = i.next();

            String name = (String)player.get("name");
            if (!hasText(name))
                throw new IllegalStateException("name must have a value");
            String uuidString = (String)player.get("uuid");
            if (uuidString == null) {
                // Needs migrating
                UuidDisplayName udn = resolved.get(name.toLowerCase());
                if (udn == null) {
                    // Couldn't be resolved, remove from list
                    i.remove();
                    warn(plugin, "Unable to migrate '%s' -- failed to lookup UUID", name);
                }
                else {
                    player.put("uuid", canonicalizeUuid(udn.getUuid()));
                    player.put("name", udn.getDisplayName());
                }
            }
        }
    }

    private void migrateGroups(List<Map<String, Object>> groups, Map<String, UuidDisplayName> resolved) {
        for (Map<String, Object> group : groups) {
            List<String> memberNames = (List<String>)group.get("members"); // Guaranteed to be non-empty list of strings due to prepareGroups
            List<Map<String, Object>> memberMaps = new ArrayList<>();
            for (String username : memberNames) {
                UuidDisplayName udn = resolved.get(username.toLowerCase());
                if (udn != null) {
                    Map<String, Object> memberMap = new LinkedHashMap<>();
                    memberMap.put("uuid", canonicalizeUuid(udn.getUuid()));
                    memberMap.put("name", udn.getDisplayName());
                    memberMaps.add(memberMap);
                }
                else {
                    warn(plugin, "Unable to migrate '%s' (member of '%s') -- failed to lookup UUID", username, group.get("name"));
                }
            }
            // Replace with migrated list
            group.put("members", memberMaps);
            
            // Migrate temp members
            List<Map<String, Object>> tempMembers = (List<Map<String, Object>>)group.get("tempmembers");
            if (tempMembers == null) // backwards compat
                tempMembers = Collections.emptyList();
            for (Iterator<Map<String, Object>> i = tempMembers.iterator(); i.hasNext();) {
                Map<String, Object> tempMember = i.next();
                String name = (String)tempMember.get("member");
                if (!hasText(name))
                    throw new IllegalStateException("member must have a value");
                if (!tempMember.containsKey("uuid")) {
                    UuidDisplayName udn = resolved.get(name.toLowerCase());
                    if (udn != null) {
                        tempMember.put("uuid", canonicalizeUuid(udn.getUuid()));
                        tempMember.put("name", udn.getDisplayName());
                        tempMember.remove("member");
                    }
                    else {
                        i.remove();
                        warn(plugin, "Unable to migrate '%s' (member of '%s') -- failed to lookup UUID", name, group.get("name"));
                    }
                }
            }
        }
    }

}
