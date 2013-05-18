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
package org.tyrannyofheaven.bukkit.zPermissions.util;

import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.quoteArgForCommand;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.xml.bind.DatatypeConverter;

import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.zPermissions.model.EntityMetadata;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.storage.StorageStrategy;

/**
 * Creates a dump file containing commands that can re-create the persistent
 * state.
 * 
 * @author zerothangel
 */
public class ModelDumper {

    private final Plugin plugin;

    private final StorageStrategy storageStrategy;

    public ModelDumper(StorageStrategy storageStrategy, Plugin plugin) {
        this.storageStrategy = storageStrategy;
        this.plugin = plugin;
    }

    public void dump(File outFile) throws FileNotFoundException {
        final PrintWriter out = new PrintWriter(outFile);
        try {
            storageStrategy.getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
                @Override
                public void doInTransactionWithoutResult() throws Exception {
                    // Header
                    out.println(String.format("# Dumped by %s %s on %s",
                            plugin.getDescription().getName(),
                            plugin.getDescription().getVersion(),
                            new Date()));
                    // Dump players first
                    List<PermissionEntity> players = sortPlayers(storageStrategy.getDao().getEntities(false));
                    for (PermissionEntity entity : players) {
                        out.println(String.format("# Player %s", entity.getDisplayName()));
                        dumpPermissions(out, entity);
                        dumpMetadata(out, entity);
                    }
                    // Dump groups
                    List<PermissionEntity> groups = sortGroups(storageStrategy.getDao().getEntities(true));
                    for (PermissionEntity entity : groups) {
                        out.println(String.format("# Group %s", entity.getDisplayName()));
                        out.println(String.format("permissions group %s create", quoteArgForCommand(entity.getDisplayName())));
                        dumpPermissions(out, entity);
                        dumpMetadata(out, entity);
                        out.println(String.format("permissions group %s setweight %d",
                                quoteArgForCommand(entity.getDisplayName()),
                                entity.getPriority()));
                        if (entity.getParent() != null) {
                            out.println(String.format("permissions group %s setparent %s",
                                    quoteArgForCommand(entity.getDisplayName()),
                                    quoteArgForCommand(entity.getParent().getDisplayName())));
                        }
                        // Dump memberships
                        for (Membership membership : storageStrategy.getDao().getMembers(entity.getName())) {
                            if (membership.getExpiration() == null) {
                                out.println(String.format("permissions group %s add %s",
                                        quoteArgForCommand(entity.getDisplayName()),
                                        quoteArgForCommand(membership.getMember())));
                            }
                            else {
                                Calendar cal = Calendar.getInstance();
                                cal.setTime(membership.getExpiration());
                                out.println(String.format("permissions group %s add %s %s",
                                        quoteArgForCommand(entity.getDisplayName()),
                                        quoteArgForCommand(membership.getMember()),
                                        quoteArgForCommand(DatatypeConverter.printDateTime(cal))));
                            }
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
        for (Entry e : Utils.sortPermissions(entity.getPermissions())) {
            out.println(String.format("permissions %s %s set %s %s",
                    (entity.isGroup() ? "group" : "player"),
                    quoteArgForCommand(entity.getDisplayName()),
                    quoteArgForCommand(String.format("%s%s%s",
                            (e.getRegion() == null ? "" : e.getRegion().getName() + "/"),
                            (e.getWorld() == null ? "" : e.getWorld().getName() + ":"),
                            e.getPermission())),
                    e.isValue()));
        }
    }

    private void dumpMetadata(PrintWriter out, PermissionEntity entity) {
        for (EntityMetadata me : Utils.sortMetadata(entity.getMetadata())) {
            Object value = me.getValue();
            String suffix = "";
            if (value instanceof Long)
                suffix = "int";
            else if (value instanceof Double)
                suffix = "real";
            else if (value instanceof Boolean)
                suffix = "bool";
            out.println(String.format("permissions %s %s metadata set%s %s %s",
                    (entity.isGroup() ? "group" : "player"),
                    quoteArgForCommand(entity.getDisplayName()),
                    suffix,
                    quoteArgForCommand(me.getName()),
                    (value instanceof String ? quoteArgForCommand((String)value) : value)));
        }
    }

    private List<PermissionEntity> sortPlayers(Collection<PermissionEntity> players) {
        List<PermissionEntity> result = new ArrayList<PermissionEntity>(players);
        // Just sort alphabetically
        Collections.sort(result, Utils.PERMISSION_ENTITY_ALPHA_COMPARATOR);
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
            Collections.sort(children, Utils.PERMISSION_ENTITY_ALPHA_COMPARATOR);

            scanList.addAll(children);
        }

        return result;
    }

}
