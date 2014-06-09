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
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.util.ToHMessageUtils;
import org.tyrannyofheaven.bukkit.util.ToHStringUtils;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.HelpBuilder;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Require;
import org.tyrannyofheaven.bukkit.util.command.Session;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.util.uuid.CommandUuidResolver;
import org.tyrannyofheaven.bukkit.util.uuid.CommandUuidResolverHandler;
import org.tyrannyofheaven.bukkit.zPermissions.PermissionsResolver;
import org.tyrannyofheaven.bukkit.zPermissions.RefreshCause;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsConfig;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsCore;
import org.tyrannyofheaven.bukkit.zPermissions.dao.MissingGroupException;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.storage.StorageStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.util.MetadataConstants;
import org.tyrannyofheaven.bukkit.zPermissions.util.Utils;

/**
 * Handler for player sub-commands. Expects the CommandSession to contain the
 * name of the player in <code>entityName</code>.
 * 
 * @author asaddi
 */
public class PlayerCommands extends CommonCommands {

    PlayerCommands(ZPermissionsCore core, StorageStrategy storageStrategy, PermissionsResolver resolver, ZPermissionsConfig config, Plugin plugin, CommandUuidResolver uuidResolver) {
        super(core, storageStrategy, resolver, config, plugin, uuidResolver, false);
    }

    // Common commands

    @Command(value="get", description="View a permission")
    @Require("zpermissions.player.view")
    public void get(CommandSender sender, final @Session("entityName") String name, @Option("permission") String permission) {
        super._get(sender, name, permission);
    }

    @Command(value="set", description="Set a permission")
    @Require("zpermissions.player.manage")
    public void set(CommandSender sender, final @Session("entityName") String name, @Option("permission") String permission, final @Option(value="value", optional=true) Boolean value) {
        super._set(sender, name, permission, value);
    }

    @Command(value="unset", description="Remove a permission")
    @Require("zpermissions.player.manage")
    public void unset(CommandSender sender, final @Session("entityName") String name, @Option("permission") String permission) {
        super._unset(sender, name, permission);
    }

    @Command(value="purge", description="Delete this player") // doh!
    @Require("zpermissions.player.manage")
    public void delete(CommandSender sender, final @Session("entityName") String name) {
        super._delete(sender, name);
    }

    @Command(value="dump", description="Display permissions for this player", varargs="region...")
    @Require("zpermissions.player.view")
    public void dump(CommandSender sender, final @Session("entityName") String name, @Option(value={"-w", "--world"}, valueName="world", completer="world") String worldName, @Option(value={"-f", "--filter"}, valueName="filter") String filter, String[] regionNames) {
        super._dump(sender, name, worldName, filter, regionNames);
    }

    @Command(value="diff", description="Compare effective permissions of this player with another", varargs="region...")
    @Require("zpermissions.player.view")
    public void diff(CommandSender sender, final @Session("entityName") String name, @Option(value={"-w", "--world"}, valueName="world", completer="world") String worldName, @Option(value={"-f", "--filter"}, valueName="filter") String filter, @Option("other") final String otherName, String[] regionNames) {
        super._diff(sender, name, worldName, filter, otherName, regionNames);
    }

    @Command(value={"metadata", "meta", "md"}, description="Metadata-related commands")
    @Require({"zpermissions.player.view", "zpermissions.player.manage", "zpermissions.player.chat"})
    public MetadataCommands metadata(HelpBuilder helpBuilder, CommandSender sender, String[] args) {
        return super._metadata(helpBuilder, sender, args);
    }

    @Command(value="prefix", description="Set chat prefix for this player")
    @Require("zpermissions.player.chat")
    public void prefix(CommandSender sender, @Session("entityName") String name, @Option(value="prefix", optional=true) String prefix, String[] rest) {
        super._prefix(sender, name, prefix, rest);
    }

    @Command(value="suffix", description="Set chat suffix for this player")
    @Require("zpermissions.player.chat")
    public void suffix(CommandSender sender, @Session("entityName") String name, @Option(value="suffix", optional=true) String suffix, String[] rest) {
        super._suffix(sender, name, suffix, rest);
    }

    // Player-specific commands

    @Command(value="groups", description="List groups this player is a member of")
    @Require("zpermissions.player.view")
    public void getGroups(CommandSender sender, @Session("entityName") String name) {
        uuidResolver.resolveUsername(sender, name, false, new CommandUuidResolverHandler() {
            @Override
            public void process(CommandSender sender, String name, UUID uuid, boolean group) {
                getGroups(sender, uuid, name);
            }
        });
    }

    private void getGroups(CommandSender sender, UUID uuid, String name) {
        List<Membership> memberships = storageStrategy.getDao().getGroups(uuid);
        Collections.reverse(memberships); // Order from highest to lowest

        String groups = Utils.displayGroups(resolver.getDefaultGroup(), memberships);

        sendMessage(sender, colorize("{AQUA}%s{YELLOW} is a member of: %s"), name, groups);
    }

    @Command(value={"setgroup", "group"}, description="Set this player's singular group")
    @Require("zpermissions.player.manage")
    public void setGroup(CommandSender sender, final @Session("entityName") String playerName, final @Option({"-a", "--add"}) boolean add, final @Option({"-A", "--add-no-reset"}) boolean addNoReset, final @Option(value="group", completer="group") String groupName, final @Option(value="duration/timestamp", optional=true) String duration, final String[] args) {
        uuidResolver.resolveUsername(sender, playerName, false, new CommandUuidResolverHandler() {
            @Override
            public void process(CommandSender sender, String name, UUID uuid, boolean group) {
                setGroup(sender, uuid, name, add, addNoReset, groupName, duration, args);
            }
        });
    }

    private void setGroup(CommandSender sender, final UUID uuid, final String playerName, final boolean add, final boolean addNoReset, final String groupName, String duration, String[] args) {
        final Date expiration = Utils.parseDurationTimestamp(duration, args);

        try {
            storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
                @Override
                public void doInTransactionWithoutResult() throws Exception {
                    Date newExpiration = handleExtendExpiration(groupName, uuid, playerName, add, addNoReset, expiration);

                    storageStrategy.getDao().setGroup(uuid, playerName, groupName, newExpiration);
                }
            });
        }
        catch (MissingGroupException e) {
            handleMissingGroup(sender, e);
            return;
        }

        sendMessage(sender, colorize("{AQUA}%s{YELLOW}'s group set to {DARK_GREEN}%s"), playerName, groupName);
        core.invalidateMetadataCache(playerName, uuid, false);
        core.refreshPlayer(uuid, RefreshCause.GROUP_CHANGE);
        
        if (expiration != null)
            core.refreshExpirations(uuid);
    }

    @Command(value={"addgroup", "add"}, description="Add this player to a group")
    @Require("zpermissions.player.manage")
    public void addGroup(CommandSender sender, @Session("entityName") String playerName, @Option({"-a", "--add"}) boolean add, final @Option({"-A", "--add-no-reset"}) boolean addNoReset, @Option(value="group", completer="group") String groupName, @Option(value="duration/timestamp", optional=true) String duration, String[] args) {
        addGroupMember(sender, groupName, playerName, duration, args, add, addNoReset);
    }

    @Command(value={"removegroup", "rmgroup", "remove", "rm"}, description="Remove this player from a group")
    @Require("zpermissions.player.manage")
    public void removeGroup(CommandSender sender, @Session("entityName") String playerName, @Option(value="group", completer="group") String groupName) {
        removeGroupMember(sender, groupName, playerName);
    }

    @Command(value={"show", "sh"}, description="Show information about a player")
    @Require("zpermissions.player.view")
    public void show(CommandSender sender, @Session("entityName") String playerName, final @Option(value={"-f", "--filter"}, valueName="filter") String filter) {
        uuidResolver.resolveUsername(sender, playerName, false, new CommandUuidResolverHandler() {
            @Override
            public void process(CommandSender sender, String name, UUID uuid, boolean group) {
                show(sender, uuid, name, filter);
            }
        });
    }

    private void show(CommandSender sender, UUID uuid, String playerName, String filter) {
        PermissionEntity entity = storageStrategy.getDao().getEntity(playerName, uuid, false);

        if (entity == null || entity.getPermissions().isEmpty()) {
            sendMessage(sender, colorize("{RED}Player has no declared permissions."));
            return;
        }

        List<String> lines = new ArrayList<>();
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
    @Require("zpermissions.player.manage")
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

        if (checkDynamicPermission(sender, permission)) return;

        player.addAttachment(plugin, permission, value == null ? true : value, ToHUtils.TICKS_PER_SECOND * timeout);

        sendMessage(sender, colorize("{GOLD}%s{YELLOW} set to {GREEN}%s{YELLOW} for {AQUA}%s{YELLOW} for %d second%s"), permission, value == null ? Boolean.TRUE : value, player.getName(), timeout, timeout == 1 ? "" : "s");
    }

    @Command(value="has", description="Bukkit hasPermission() check")
    @Require("zpermissions.player.view")
    public void has(CommandSender sender, @Session("entityName") String playerName, @Option("permission") String permission) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            sendMessage(sender, colorize("{RED}Player is not online."));
            abortBatchProcessing();
            return;
        }

        sendMessage(sender, colorize("{GREEN}%s"), player.hasPermission(permission));
    }

    @Command(value="settrack", description="Set track which determines primary group for Vault")
    @Require("zpermissions.player.chat")
    public void settrack(CommandSender sender, @Session("entityName") String playerName, @Option(value="track", optional=true, completer="track") String track) {
        if (ToHStringUtils.hasText(track)) {
            getMetadataCommands().set(sender, playerName, MetadataConstants.PRIMARY_GROUP_TRACK_KEY, track, new String[0]);
        }
        else {
            getMetadataCommands().unset(sender, playerName, MetadataConstants.PRIMARY_GROUP_TRACK_KEY);
        }
    }

    @Command(value={"clone", "copy", "cp"}, description="Clone this player")
    @Require("zpermissions.player.manage")
    public void clone(CommandSender sender, @Session("entityName") String playerName, @Option("new-player") String destination) {
        super.clone(sender, playerName, destination, false);
    }

}
