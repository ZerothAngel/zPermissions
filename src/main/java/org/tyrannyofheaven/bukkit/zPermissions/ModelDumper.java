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
package org.tyrannyofheaven.bukkit.zPermissions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

public class ModelDumper {

    private final ZPermissionsPlugin plugin;
    
    ModelDumper(ZPermissionsPlugin plugin) {
        this.plugin = plugin;
    }

    public void dump(File outFile) throws FileNotFoundException {
        final PrintWriter out = new PrintWriter(outFile);
        try {
            plugin.getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
                @Override
                public void doInTransactionWithoutResult() throws Exception {
                    // Header
                    out.println(String.format("# Dumped by %s %s on %s",
                            plugin.getDescription().getName(),
                            plugin.getDescription().getVersion(),
                            new Date()));
                    // Dump players first
                    List<PermissionEntity> players = sortPlayers(plugin.getDao().getEntities(false));
                    for (PermissionEntity entity : players) {
                        out.println(String.format("# Player %s", entity.getDisplayName()));
                        dumpPermissions(out, entity);
                    }
                    // Dump groups
                    List<PermissionEntity> groups = sortGroups(plugin.getDao().getEntities(true));
                    for (PermissionEntity entity : groups) {
                        out.println(String.format("# Group %s", entity.getDisplayName()));
                        out.println(String.format("permissions group %s create", entity.getDisplayName()));
                        dumpPermissions(out, entity);
                        out.println(String.format("permissions group %s setpriority %d",
                                entity.getDisplayName(),
                                entity.getPriority()));
                        if (entity.getParent() != null) {
                            out.println(String.format("permissions group %s setparent %s",
                                    entity.getDisplayName(),
                                    entity.getParent().getDisplayName()));
                        }
                        // Dump memberships
                        for (String playerName : plugin.getDao().getMembers(entity.getName())) {
                            out.println(String.format("permissions group %s add %s",
                                    entity.getDisplayName(),
                                    playerName));
                        }
                    }
                }
            });
        }
        finally {
            out.close();
        }
    }

    // Dump permissions for a player or group
    private void dumpPermissions(final PrintWriter out, PermissionEntity entity) {
        for (Entry e : sortPermissions(entity.getPermissions())) {
            out.println(String.format("permissions %s %s set %s%s%s %s",
                    (entity.isGroup() ? "group" : "player"),
                    entity.getDisplayName(),
                    (e.getRegion() == null ? "" : e.getRegion().getName() + "/"),
                    (e.getWorld() == null ? "" : e.getWorld().getName() + ":"),
                    e.getPermission(),
                    e.isValue()));
        }
    }

    private List<PermissionEntity> sortPlayers(Collection<PermissionEntity> players) {
        List<PermissionEntity> result = new ArrayList<PermissionEntity>(players);
        // Just sort alphabetically
        Collections.sort(result, new Comparator<PermissionEntity>() {
            @Override
            public int compare(PermissionEntity a, PermissionEntity b) {
                return a.getDisplayName().compareTo(b.getDisplayName());
            }
        });
        return result;
    }

    private List<PermissionEntity> sortGroups(Collection<PermissionEntity> groups) {
        Queue<PermissionEntity> scanList = new LinkedList<PermissionEntity>();
        
        // Seed with parent-less groups
        for (PermissionEntity group : groups) {
            if (group.getParent() == null)
                scanList.add(group);
        }

        List<PermissionEntity> result = new ArrayList<PermissionEntity>(groups.size());

        // BFS from queue to get total ordering
        while (!scanList.isEmpty()) {
            PermissionEntity group = scanList.remove();
            
            // Add to result
            result.add(group);
            
            // Grab children and add to end of scanList
            List<PermissionEntity> children = new ArrayList<PermissionEntity>(group.getChildren().size());
            children.addAll(group.getChildren());
            
            // Sort children alphabetically
            Collections.sort(children, new Comparator<PermissionEntity>() {
                @Override
                public int compare(PermissionEntity a, PermissionEntity b) {
                    return a.getDisplayName().compareTo(b.getDisplayName());
                }
            });

            scanList.addAll(children);
        }

        return result;
    }

    private List<Entry> sortPermissions(Collection<Entry> entries) {
        List<Entry> result = new ArrayList<Entry>(entries);
        Collections.sort(result, new Comparator<Entry>() {
            @Override
            public int compare(Entry a, Entry b) {
                if (a.getRegion() != null && b.getRegion() == null)
                    return 1;
                else if (a.getRegion() == null && b.getRegion() != null)
                    return -1;
                else if (a.getRegion() != null && b.getRegion() != null) {
                    int regions = a.getRegion().getName().compareTo(b.getRegion().getName());
                    if (regions != 0) return regions;
                }

                if (a.getWorld() != null && b.getWorld() == null)
                    return 1;
                else if (a.getWorld() == null && b.getWorld() != null)
                    return -1;
                else if (a.getWorld() != null && b.getWorld() != null) {
                    int worlds = a.getWorld().getName().compareTo(b.getWorld().getName());
                    if (worlds != 0) return worlds;
                }

                return a.getPermission().compareTo(b.getPermission());
            }
        });
        return result;
    }

}
