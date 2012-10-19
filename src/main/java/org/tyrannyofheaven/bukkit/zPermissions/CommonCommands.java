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

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Session;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.zPermissions.dao.MissingGroupException;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;

/**
 * Handler for common commands between "/permissions group" and
 * "/permissions player"
 * 
 * @author zerothangel
 */
public abstract class CommonCommands {

    // Parent plugin
    protected final ZPermissionsPlugin plugin;

    // true if this is handling groups
    private final boolean group;

    /**
     * Instantiate this handler.
     * 
     * @param group true if this is handling groups
     */
    protected CommonCommands(ZPermissionsPlugin plugin, boolean group) {
        this.plugin = plugin;
        this.group = group;
    }

    @Command(value="get", description="View a permission")
    public void get(CommandSender sender, final @Session("entityName") String name, @Option(value="permission") String permission) {
        // Get world/permission
        final WorldPermission wp = new WorldPermission(permission);

        // Read entry from DAO, if any
        Boolean result = plugin.getRetryingTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                return plugin.getDao().getPermission(name, group, wp.getRegion(), wp.getWorld(), wp.getPermission());
            }
        });
        
        if (result == null) {
            sendMessage(sender, colorize("%s%s{YELLOW} does not set {GOLD}%s"), group ? ChatColor.DARK_GREEN : ChatColor.AQUA, name, permission);
            if (!group)
                plugin.checkPlayer(sender, name);
        }
        else {
            sendMessage(sender, colorize("%s%s{YELLOW} sets {GOLD}%s{YELLOW} to {GREEN}%s"), group ? ChatColor.DARK_GREEN : ChatColor.AQUA, name, permission, result);
        }
    }

    @Command(value="set", description="Set a permission")
    public void set(CommandSender sender, final @Session("entityName") String name, @Option("permission") String permission, final @Option(value="value", optional=true) Boolean value) {
        // Get world/permission
        final WorldPermission wp = new WorldPermission(permission);
    
        // Set permission.
        try {
            plugin.getRetryingTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
                @Override
                public void doInTransactionWithoutResult() throws Exception {
                    plugin.getDao().setPermission(name, group, wp.getRegion(), wp.getWorld(), wp.getPermission(), value == null ? Boolean.TRUE : value);
                }
            });
        }
        catch (MissingGroupException e) {
            handleMissingGroup(sender, e);
            return;
        }
    
        sendMessage(sender, colorize("{GOLD}%s{YELLOW} set to {GREEN}%s{YELLOW} for %s%s"), permission, value == null ? Boolean.TRUE : value, group ? ChatColor.DARK_GREEN : ChatColor.AQUA, name);
        if (!group) {
            plugin.checkPlayer(sender, name);
            plugin.refreshPlayer(name);
        }
        else {
            plugin.refreshAffectedPlayers(name);
        }
    }

    @Command(value="unset", description="Remove a permission")
    public void unset(CommandSender sender, final @Session("entityName") String name, @Option("permission") String permission) {
        // Get world/permission
        final WorldPermission wp = new WorldPermission(permission);
    
        // Delete permission entry.
        Boolean result = plugin.getRetryingTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                return plugin.getDao().unsetPermission(name, group, wp.getRegion(), wp.getWorld(), wp.getPermission());
            }
        });

        if (result) {
            sendMessage(sender, colorize("{GOLD}%s{YELLOW} unset for %s%s"), permission, group ? ChatColor.DARK_GREEN : ChatColor.AQUA, name);
            if (group)
                plugin.refreshAffectedPlayers(name);
            else
                plugin.refreshPlayer(name);
        }
        else {
            sendMessage(sender, colorize("%s%s{RED} does not set {GOLD}%s"), group ? ChatColor.DARK_GREEN : ChatColor.AQUA, name, permission);
            if (!group)
                plugin.checkPlayer(sender, name);
        }
    }

    @Command(value="purge", description="Delete this group or player") // doh!
    public void delete(CommandSender sender, final @Session("entityName") String name) {
        boolean result = plugin.getRetryingTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                return plugin.getDao().deleteEntity(name, group);
            }
        });
        
        if (result) {
            sendMessage(sender, colorize("{YELLOW}%s %s%s{YELLOW} deleted"),
                    (group ? "Group" : "Player"),
                    (group ? ChatColor.DARK_GREEN : ChatColor.AQUA),
                    name);
            if (group)
                plugin.refreshAffectedPlayers(name);
            else
                plugin.refreshPlayer(name);
        }
        else
            sendMessage(sender, colorize("{RED}%s not found."), group ? "Group" : "Player");
    }

    protected void displayEntry(CommandSender sender, Entry e) {
        sendMessage(sender, colorize("{DARK_GREEN}- {GOLD}%s%s%s{DARK_GREEN}: {GREEN}%s"),
                (e.getRegion() == null ? "" : e.getRegion().getName() + colorize("{DARK_GREEN}/{GOLD}")),
                (e.getWorld() == null ? "" : e.getWorld().getName() + colorize("{DARK_GREEN}:{GOLD}")),
                e.getPermission(),
                e.isValue());
    }

    protected void handleMissingGroup(CommandSender sender, MissingGroupException e) {
        sendMessage(sender, colorize("{RED}Group {DARK_GREEN}%s{RED} does not exist."), e.getGroupName());
    }

}
