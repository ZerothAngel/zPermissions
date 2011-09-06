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
package org.tyrannyofheaven.bukkit.zPermissions;

import static org.tyrannyofheaven.bukkit.util.ToHUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.sendMessage;

import java.util.Iterator;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Session;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

public class PlayerCommand extends CommonCommand {

    public PlayerCommand() {
        super(false);
    }

    @Command(value="groups", description="List groups this player is a member of")
    public void getGroups(ZPermissionsPlugin plugin, CommandSender sender, @Session("entityName") String name) {
        List<PermissionEntity> groups = plugin.getDao().getGroups(name);

        // Add default group if needed and available
        if (groups.isEmpty()) {
            PermissionEntity defaultGroup = plugin.getDao().getEntity(plugin.getDefaultGroup(), true);
            if (defaultGroup != null)
                groups.add(defaultGroup);
        }

        if (groups.isEmpty()) {
            sendMessage(sender, colorize("{YELLOW}Player is not a member of any groups."));
        }
        else {
            StringBuilder sb = new StringBuilder();
            for (Iterator<PermissionEntity> i = groups.iterator(); i.hasNext();) {
                PermissionEntity group = i.next();
                sb.append(ChatColor.DARK_GREEN);
                sb.append(group.getDisplayName());
                if (i.hasNext()) {
                    sb.append(ChatColor.YELLOW);
                    sb.append(", ");
                }
            }
            sendMessage(sender, colorize("{AQUA}%s{YELLOW} is a member of: %s"), name, sb.toString());
        }
    }

    @Command(value={"setgroup", "group"}, description="Set this player's singular group")
    public void setGroup(final ZPermissionsPlugin plugin, CommandSender sender, final @Session("entityName") String playerName, final @Option("group") String groupName) {
        plugin.getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                plugin.getDao().setGroup(playerName, groupName);
            }
        });

        sendMessage(sender, colorize("{AQUA}%s{YELLOW}'s group set to {DARK_GREEN}%s"), playerName, groupName);
        plugin.refreshPlayer(playerName);
    }

    @Command(value={"show", "sh"}, description="Show information about a player")
    public void show(ZPermissionsPlugin plugin, CommandSender sender, @Session("entityName") String playerName) {
        PermissionEntity entity = plugin.getDao().getEntity(playerName, false);

        if (entity == null || entity.getPermissions().isEmpty()) {
            sendMessage(sender, colorize("{RED}Player has no declared permissions."));
            return;
        }
        
        sendMessage(sender, colorize("{YELLOW}Declared permissions for {AQUA}%s{YELLOW}:"), entity.getDisplayName());
        for (Entry e : entity.getPermissions()) {
            sendMessage(sender, colorize("{DARK_GREEN}- {GOLD}%s%s{DARK_GREEN}: {GREEN}%s"), e.getWorld() == null ? "" : e.getWorld().getName() + ":", e.getPermission(), e.isValue());
        }
    }

}
