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
package org.tyrannyofheaven.bukkit.zPermissions.command;

import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;
import static org.tyrannyofheaven.bukkit.util.command.reader.CommandReader.abortBatchProcessing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.util.ToHMessageUtils;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Session;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.zPermissions.PermissionsResolver;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsConfig;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsCore;
import org.tyrannyofheaven.bukkit.zPermissions.dao.MissingGroupException;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.storage.StorageStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.util.Utils;

/**
 * Handler for player sub-commands. Expects the CommandSession to contain the
 * name of the player in <code>entityName</code>.
 * 
 * @author asaddi
 */
public class PlayerCommands extends CommonCommands {

    PlayerCommands(ZPermissionsCore core, StorageStrategy storageStrategy, PermissionsResolver resolver, ZPermissionsConfig config, Plugin plugin) {
        super(core, storageStrategy, resolver, config, plugin, false);
    }

    @Command(value="groups", description="List groups this player is a member of")
    public void getGroups(CommandSender sender, @Session("entityName") String name) {
        List<Membership> memberships = storageStrategy.getDao().getGroups(name);
        Collections.reverse(memberships); // Order from highest to lowest

        String groups = Utils.displayGroups(resolver.getDefaultGroup(), memberships);

        sendMessage(sender, colorize("{AQUA}%s{YELLOW} is a member of: %s"), name, groups);
        
        if (memberships.isEmpty())
            Utils.checkPlayer(sender, name);
    }

    @Command(value={"setgroup", "group"}, description="Set this player's singular group")
    public void setGroup(CommandSender sender, final @Session("entityName") String playerName, final @Option(value="group", completer="group") String groupName, @Option(value="duration/timestamp", optional=true) String duration, String[] args) {
        final Date expiration = Utils.parseDurationTimestamp(duration, args);

        try {
            storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
                @Override
                public void doInTransactionWithoutResult() throws Exception {
                    storageStrategy.getDao().setGroup(playerName, groupName, expiration);
                }
            });
        }
        catch (MissingGroupException e) {
            handleMissingGroup(sender, e);
            return;
        }

        sendMessage(sender, colorize("{AQUA}%s{YELLOW}'s group set to {DARK_GREEN}%s"), playerName, groupName);
        Utils.checkPlayer(sender, playerName);
        core.refreshPlayer(playerName);
        
        if (expiration != null)
            core.refreshExpirations(playerName);
    }

    @Command(value={"show", "sh"}, description="Show information about a player")
    public void show(CommandSender sender, @Session("entityName") String playerName, @Option(value={"-f", "--filter"}, valueName="filter") String filter) {
        PermissionEntity entity = storageStrategy.getDao().getEntity(playerName, false);

        if (entity == null || entity.getPermissions().isEmpty()) {
            sendMessage(sender, colorize("{RED}Player has no declared permissions."));
            Utils.checkPlayer(sender, playerName);
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
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            sendMessage(sender, colorize("{RED}Player is not online."));
            abortBatchProcessing();
            return;
        }
        
        if (timeout == null)
            timeout = config.getDefaultTempPermissionTimeout();
        if (timeout <= 0) {
            sendMessage(sender, colorize("{RED}Invalid timeout."));
            abortBatchProcessing();
            return;
        }

        player.addAttachment(plugin, permission, value == null ? true : value, ToHUtils.TICKS_PER_SECOND * timeout);

        sendMessage(sender, colorize("{GOLD}%s{YELLOW} set to {GREEN}%s{YELLOW} for {AQUA}%s{YELLOW} for %d second%s"), permission, value == null ? Boolean.TRUE : value, player.getName(), timeout, timeout == 1 ? "" : "s");
    }

    @Command(value="has", description="Bukkit hasPermission() check")
    public void has(CommandSender sender, @Session("entityName") String playerName, @Option("permission") String permission) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            sendMessage(sender, colorize("{RED}Player is not online."));
            abortBatchProcessing();
            return;
        }

        sendMessage(sender, colorize("{GREEN}%s"), player.hasPermission(permission));
    }

}
