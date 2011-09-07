/*
 * Copyright 2011 Allan Saddi <allan@saddi.com>
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
import static org.tyrannyofheaven.bukkit.util.ToHUtils.delimitedString;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.sendMessage;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Session;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.zPermissions.dao.DaoException;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

/**
 * Handler for group commands. Expects the group name to be in the CommandSession
 * as <code>entityName</code>.
 * 
 * @author asaddi
 */
public class GroupCommand extends CommonCommand {

    public GroupCommand() {
        super(true);
    }

    @Command(value="add", description="Add a player to a group")
    public void addMember(final ZPermissionsPlugin plugin, CommandSender sender, final @Session("entityName") String groupName, final @Option("player") String playerName) {
        // Add player to group. Will always succeed.
        plugin.getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                plugin.getDao().addMember(groupName, playerName);
            }
        });

        sendMessage(sender, colorize("{AQUA}%s{YELLOW} added to {DARK_GREEN}%s"), playerName, groupName);
        checkPlayer(plugin, sender, playerName);
        plugin.refreshPlayer(playerName);
    }

    @Command(value={"remove", "rm"}, description="Remove a player from a group")
    public void removeMember(final ZPermissionsPlugin plugin, CommandSender sender, final @Session("entityName") String groupName, final @Option("player") String playerName) {
        // Remove player from group
        Boolean result = plugin.getTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                return plugin.getDao().removeMember(groupName, playerName);
            }
        });

        if (result) {
            sendMessage(sender, colorize("{AQUA}%s{YELLOW} removed from {DARK_GREEN}%s"), playerName, groupName);
            plugin.refreshPlayer(playerName);
        }
        else {
            sendMessage(sender, colorize("{DARK_GREEN}%s{RED} does not exist or {AQUA}%s{RED} is not a member"), groupName, playerName);
            checkPlayer(plugin, sender, playerName);
        }
    }

    @Command(value={"show", "sh"}, description="Show information about a group")
    public void show(ZPermissionsPlugin plugin, CommandSender sender, @Session("entityName") String groupName) {
        PermissionEntity entity = plugin.getDao().getEntity(groupName, true);

        if (entity != null) {
            sendMessage(sender, colorize("{YELLOW}Declared permissions for {DARK_GREEN}%s{YELLOW}:"), entity.getDisplayName());
            sendMessage(sender, colorize("{YELLOW}Priority: {GREEN}%s"), entity.getPriority());
            if (entity.getParent() != null) {
                sendMessage(sender, colorize("{YELLOW}Parent: {DARK_GREEN}%s"), entity.getParent().getDisplayName());
            }
            for (Entry e : entity.getPermissions()) {
                sendMessage(sender, colorize("{DARK_GREEN}- {GOLD}%s%s{DARK_GREEN}: {GREEN}%s"), e.getWorld() == null ? "" : e.getWorld().getName() + ":", e.getPermission(), e.isValue());
            }
        }

        if (entity == null || entity.getPermissions().isEmpty()) {
            sendMessage(sender, colorize("{RED}Group has no declared permissions."));
            return;
        }
    }

    @Command(value={"setparent", "parent"}, description="Set a group's parent")
    public void setParent(final ZPermissionsPlugin plugin, CommandSender sender, final @Session("entityName") String groupName, final @Option(value="parent", optional=true) String parentName) {
        try {
            // Set parent. Creates group and/or parent if missing.
            plugin.getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
                @Override
                public void doInTransactionWithoutResult() throws Exception {
                    plugin.getDao().setParent(groupName, parentName);
                }
            });
        }
        catch (DaoException e) {
            // Most likely due to inheritance cycle
            sendMessage(sender, colorize("{RED}%s"), e.getMessage());
            return;
        }

        sendMessage(sender, colorize("{DARK_GREEN}%s{YELLOW}'s parent is now {DARK_GREEN}%s"), groupName, parentName);
        plugin.refreshPlayers();
    }

    @Command(value={"setpriority", "priority"}, description="Set a group's priority")
    public void setPriority(final ZPermissionsPlugin plugin, CommandSender sender, final @Session("entityName") String groupName, final @Option("priority") int priority) {
        // Set the priority. Will not fail, creates group if necessary
        plugin.getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                plugin.getDao().setPriority(groupName, priority);
            }
        });

        sendMessage(sender, colorize("{DARK_GREEN}%s{YELLOW}'s priority is now {GREEN}%d"), groupName, priority);
        plugin.refreshPlayers();
    }

    @Command(value="members", description="List members of a group")
    public void members(ZPermissionsPlugin plugin, CommandSender sender, @Session("entityName") String groupName) {
        List<String> members = plugin.getDao().getMembers(groupName);
        
        // NB: Can't tell if group doesn't exist or if it has no members.
        if (members.isEmpty())
            sendMessage(sender, colorize("{YELLOW}Group has no members."));
        else {
            sendMessage(sender, colorize("{YELLOW}Members of {DARK_GREEN}%s{YELLOW}: {AQUA}%s"), groupName, delimitedString(ChatColor.YELLOW + ", " + ChatColor.AQUA, members));
        }
    }

}
