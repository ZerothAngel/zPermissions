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

import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;
import static org.tyrannyofheaven.bukkit.util.command.reader.CommandReader.abortBatchProcessing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.tyrannyofheaven.bukkit.util.ToHMessageUtils;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Session;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.zPermissions.dao.MissingGroupException;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

/**
 * Handler for player sub-commands. Expects the CommandSession to contain the
 * name of the player in <code>entityName</code>.
 * 
 * @author zerothangel
 */
public class PlayerCommands extends CommonCommands {

    public PlayerCommands(ZPermissionsPlugin plugin, PermissionsResolver resolver) {
        super(plugin, resolver, false);
    }

    @Command(value="groups", description="List groups this player is a member of")
    public void getGroups(CommandSender sender, @Session("entityName") String name) {
        List<String> groups = plugin.getDao().getGroups(name);

        // Add default group if needed
        if (groups.isEmpty()) {
            groups.add(plugin.getResolver().getDefaultGroup());
        }

        if (groups.isEmpty()) {
            sendMessage(sender, colorize("{YELLOW}Player is not a member of any groups."));
            plugin.checkPlayer(sender, name);
        }
        else {
            StringBuilder sb = new StringBuilder();
            for (Iterator<String> i = groups.iterator(); i.hasNext();) {
                String group = i.next();
                sb.append(ChatColor.DARK_GREEN);
                sb.append(group);
                if (i.hasNext()) {
                    sb.append(ChatColor.YELLOW);
                    sb.append(", ");
                }
            }
            sendMessage(sender, colorize("{AQUA}%s{YELLOW} is a member of: %s"), name, sb.toString());
        }
    }

    @Command(value={"setgroup", "group"}, description="Set this player's singular group")
    public void setGroup(CommandSender sender, final @Session("entityName") String playerName, final @Option(value="group", completer="group") String groupName) {
        try {
            plugin.getRetryingTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
                @Override
                public void doInTransactionWithoutResult() throws Exception {
                    plugin.getDao().setGroup(playerName, groupName);
                }
            });
        }
        catch (MissingGroupException e) {
            handleMissingGroup(sender, e);
            return;
        }

        sendMessage(sender, colorize("{AQUA}%s{YELLOW}'s group set to {DARK_GREEN}%s"), playerName, groupName);
        plugin.checkPlayer(sender, playerName);
        plugin.refreshPlayer(playerName);
    }

    @Command(value={"show", "sh"}, description="Show information about a player")
    public void show(CommandSender sender, @Session("entityName") String playerName, @Option(value={"-f", "--filter"}, valueName="filter") String filter) {
        PermissionEntity entity = plugin.getDao().getEntity(playerName, false);

        if (entity == null || entity.getPermissions().isEmpty()) {
            sendMessage(sender, colorize("{RED}Player has no declared permissions."));
            plugin.checkPlayer(sender, playerName);
            return;
        }

        List<String> lines = new ArrayList<String>();
        lines.add(String.format(colorize("{YELLOW}Declared permissions for {AQUA}%s{YELLOW}:"), entity.getDisplayName()));
        if (filter != null) {
            filter = filter.toLowerCase().trim();
            if (filter.isEmpty())
                filter = null;
        }
        for (Entry e : Utils.sortPermissions(entity.getPermissions())) {
            if (filter != null && !(
                    (e.getRegion() != null && e.getRegion().getName().contains(filter)) ||
                    (e.getWorld() != null && e.getWorld().getName().contains(filter)) ||
                    e.getPermission().contains(filter)))
                continue;
            lines.add(formatEntry(sender, e));
        }
        ToHMessageUtils.displayLines(plugin, sender, lines);
    }

    @Command(value={"settemp", "temp", "tmp"}, description="Set a temporary permission")
    public void settemp(CommandSender sender, @Session("entityName") String playerName, @Option("permission") String permission, @Option(value="value", optional=true) Boolean value, @Option(value={"-t", "--timeout"}, valueName="timeout") Integer timeout) {
        Player player = plugin.getServer().getPlayer(playerName);
        if (player == null) {
            sendMessage(sender, colorize("{RED}Player is not online."));
            abortBatchProcessing();
            return;
        }
        
        if (timeout == null)
            timeout = plugin.getDefaultTempPermissionTimeout();
        if (timeout <= 0) {
            sendMessage(sender, colorize("{RED}Invalid timeout."));
            abortBatchProcessing();
            return;
        }

        player.addAttachment(plugin, permission, value == null ? true : value, ToHUtils.TICKS_PER_SECOND * timeout);

        sendMessage(sender, colorize("{GOLD}%s{YELLOW} set to {GREEN}%s{YELLOW} for {AQUA}%s{YELLOW} for %d second%s"), permission, value == null ? Boolean.TRUE : value, player.getName(), timeout, timeout == 1 ? "" : "s");
    }

}
