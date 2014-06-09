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
package org.tyrannyofheaven.bukkit.zPermissions.util;

import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.delimitedString;
import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.quoteArgForCommand;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
 * @author asaddi
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
                    List<PermissionEntity> players = Utils.sortPlayers(storageStrategy.getDao().getEntities(false));
                    for (PermissionEntity entity : players) {
                        out.println(String.format("# Player %s", entity.getDisplayName()));
                        dumpPermissions(out, entity);
                        dumpMetadata(out, entity);
                    }
                    // Dump groups
                    List<PermissionEntity> groups = Utils.sortGroups(storageStrategy.getDao().getEntities(true));
                    for (PermissionEntity entity : groups) {
                        out.println(String.format("# Group %s", entity.getDisplayName()));
                        out.println(String.format("permissions group %s create", quoteArgForCommand(entity.getDisplayName())));
                        dumpPermissions(out, entity);
                        dumpMetadata(out, entity);
                        out.println(String.format("permissions group %s setweight %d",
                                quoteArgForCommand(entity.getDisplayName()),
                                entity.getPriority()));
                        List<PermissionEntity> parents = entity.getParents();
                        if (!parents.isEmpty()) {
                            List<String> parentNames = new ArrayList<>(parents.size());
                            for (PermissionEntity parent : parents)
                                parentNames.add(quoteArgForCommand(parent.getDisplayName()));
                            out.println(String.format("permissions group %s setparents %s",
                                    quoteArgForCommand(entity.getDisplayName()),
                                    delimitedString(" ", parentNames)));
                        }
                        // Dump memberships (NB getMembers() is already sorting them)
                        for (Membership membership : storageStrategy.getDao().getMembers(entity.getName())) {
                            if (membership.getExpiration() == null) {
                                out.println(String.format("permissions group %s add %s",
                                        quoteArgForCommand(entity.getDisplayName()),
                                        quoteArgForCommand(membership.getMember() + "/" + membership.getDisplayName())));
                            }
                            else {
                                Calendar cal = Calendar.getInstance();
                                cal.setTime(membership.getExpiration());
                                out.println(String.format("permissions group %s add %s %s",
                                        quoteArgForCommand(entity.getDisplayName()),
                                        quoteArgForCommand(membership.getMember() + "/" + membership.getDisplayName()),
                                        quoteArgForCommand(DatatypeConverter.printDateTime(cal))));
                            }
                        }
                    }
                }
            }, true);
        }
        finally {
            out.close();
        }
    }

    // Dump permissions for a player or group
    private void dumpPermissions(final PrintWriter out, PermissionEntity entity) {
        String type = entity.isGroup() ? "group" : "player";
        String name = quoteArgForCommand(entity.isGroup() ? entity.getDisplayName() : entity.getName() + "/" + entity.getDisplayName());
        for (Entry e : Utils.sortPermissions(entity.getPermissions())) {
            out.println(String.format("permissions %s %s set %s %s",
                    type,
                    name,
                    quoteArgForCommand(String.format("%s%s%s",
                            (e.getRegion() == null ? "" : e.getRegion().getName() + "/"),
                            (e.getWorld() == null ? "" : e.getWorld().getName() + ":"),
                            e.getPermission())),
                    e.isValue()));
        }
    }

    private void dumpMetadata(PrintWriter out, PermissionEntity entity) {
        String type = entity.isGroup() ? "group" : "player";
        String name = quoteArgForCommand(entity.isGroup() ? entity.getDisplayName() : entity.getName() + "/" + entity.getDisplayName());
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
                    type,
                    name,
                    suffix,
                    quoteArgForCommand(me.getName()),
                    (value instanceof String ? quoteArgForCommand((String)value) : value)));
        }
    }

}
