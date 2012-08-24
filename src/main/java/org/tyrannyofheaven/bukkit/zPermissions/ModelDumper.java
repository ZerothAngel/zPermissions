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
import java.util.Date;
import java.util.List;

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
                    List<PermissionEntity> players = plugin.getDao().getEntities(false);
                    for (PermissionEntity entity : players) {
                        out.println(String.format("# Player %s", entity.getDisplayName()));
                        dumpPermissions(out, entity);
                    }
                    // Dump groups
                    List<PermissionEntity> groups = plugin.getDao().getEntities(true);
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
        for (Entry e : entity.getPermissions()) {
            out.println(String.format("permissions %s %s set %s%s%s %s",
                    (entity.isGroup() ? "group" : "player"),
                    entity.getDisplayName(),
                    (e.getRegion() == null ? "" : e.getRegion().getName() + "/"),
                    (e.getWorld() == null ? "" : e.getWorld().getName() + ":"),
                    e.getPermission(),
                    e.isValue()));
        }
    }

}
