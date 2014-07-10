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
package org.tyrannyofheaven.bukkit.zPermissions.command;

import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.broadcastAdmin;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;
import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.delimitedString;
import static org.tyrannyofheaven.bukkit.util.command.reader.CommandReader.abortBatchProcessing;
import static org.tyrannyofheaven.bukkit.zPermissions.util.Utils.formatPlayerName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.util.ToHMessageUtils;
import org.tyrannyofheaven.bukkit.util.ToHStringUtils;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.HelpBuilder;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Require;
import org.tyrannyofheaven.bukkit.util.command.Session;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.util.uuid.CommandUuidResolver;
import org.tyrannyofheaven.bukkit.zPermissions.PermissionsResolver;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsConfig;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsCore;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionServiceException;
import org.tyrannyofheaven.bukkit.zPermissions.dao.MissingGroupException;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.storage.StorageStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.util.Utils;

/**
 * Handler for group commands. Expects the group name to be in the CommandSession
 * as <code>entityName</code>.
 * 
 * @author zerothangel
 */
public class GroupCommands extends CommonCommands {

    GroupCommands(ZPermissionsCore core, StorageStrategy storageStrategy, PermissionsResolver resolver, ZPermissionsConfig config, Plugin plugin, CommandUuidResolver uuidResolver) {
        super(core, storageStrategy, resolver, config, plugin, uuidResolver, true);
    }

    // Common commands

    @Command(value="get", description="View a permission")
    @Require("zpermissions.group.view")
    public void get(CommandSender sender, final @Session("entityName") String name, @Option("permission") String permission) {
        super._get(sender, name, permission);
    }

    @Command(value="set", description="Set a permission")
    @Require("zpermissions.group.manage")
    public void set(CommandSender sender, final @Session("entityName") String name, @Option("permission") String permission, final @Option(value="value", optional=true) Boolean value) {
        super._set(sender, name, permission, value);
    }

    @Command(value="unset", description="Remove a permission")
    @Require("zpermissions.group.manage")
    public void unset(CommandSender sender, final @Session("entityName") String name, @Option("permission") String permission) {
        super._unset(sender, name, permission);
    }

    @Command(value="purge", description="Delete this group or purge its members")
    @Require("zpermissions.group.manage")
    public void delete(CommandSender sender, final @Session("entityName") String name, @Option(value={"-m", "--members-only"}) boolean membersOnly) {
        if (!membersOnly) {
            super._delete(sender, name);
        }
        else {
            boolean result;
            try {
                result = storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallback<Boolean>() {
                    @Override
                    public Boolean doInTransaction() throws Exception {
                        List<Membership> memberships = storageStrategy.getPermissionService().getMembers(name);

                        for (Membership membership : memberships) {
                            storageStrategy.getPermissionService().removeMember(name, membership.getUuid());
                        }

                        return !memberships.isEmpty();
                    }
                });
            }
            catch (MissingGroupException e) {
                handleMissingGroup(sender, e);
                return;
            }
            
            if (result) {
                sendMessage(sender, colorize("{YELLOW}Group {DARK_GREEN}%s{YELLOW} purged of members."), name);
                core.invalidateMetadataCache(name, null, true);
                if (core.refreshAffectedPlayers(name))
                    core.refreshExpirations();
            }
            else {
                // Nothing happened for one reason or another
                sendMessage(sender, colorize("{YELLOW}Group has no members or does not exist."));
            }
        }
    }

    @Command(value="dump", description="Display permissions for this group", varargs="region...")
    @Require("zpermissions.group.view")
    public void dump(CommandSender sender, final @Session("entityName") String name, @Option(value={"-w", "--world"}, valueName="world", completer="world") String worldName, @Option(value={"-f", "--filter"}, valueName="filter") String filter, String[] regionNames) {
        super._dump(sender, name, worldName, filter, regionNames);
    }

    @Command(value="diff", description="Compare effective permissions of this group with another", varargs="region...")
    @Require("zpermissions.group.view")
    public void diff(CommandSender sender, final @Session("entityName") String name, @Option(value={"-w", "--world"}, valueName="world", completer="world") String worldName, @Option(value={"-f", "--filter"}, valueName="filter") String filter, @Option("other") final String otherName, String[] regionNames) {
        super._diff(sender, name, worldName, filter, otherName, regionNames);
    }

    @Command(value={"metadata", "meta", "md"}, description="Metadata-related commands")
    @Require({"zpermissions.group.view", "zpermissions.group.manage", "zpermissions.group.chat"})
    public MetadataCommands metadata(HelpBuilder helpBuilder, CommandSender sender, String[] args) {
        return super._metadata(helpBuilder, sender, args);
    }

    @Command(value="prefix", description="Set chat prefix for this group")
    @Require("zpermissions.group.chat")
    public void prefix(CommandSender sender, @Session("entityName") String name, @Option(value="prefix", optional=true) String prefix, String[] rest) {
        super._prefix(sender, name, prefix, rest);
    }

    @Command(value="suffix", description="Set chat suffix for this group")
    @Require("zpermissions.group.chat")
    public void suffix(CommandSender sender, @Session("entityName") String name, @Option(value="suffix", optional=true) String suffix, String[] rest) {
        super._suffix(sender, name, suffix, rest);
    }

    // Group-specific commands

    @Command(value="create", description="Create a group")
    @Require("zpermissions.group.manage")
    public void create(CommandSender sender, final @Session("entityName") String groupName) {
        boolean result = storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                return storageStrategy.getPermissionService().createGroup(groupName);
            }
        });
        
        if (result) {
            broadcastAdmin(plugin, "%s created group %s", sender.getName(), groupName);
            sendMessage(sender, colorize("{YELLOW}Group {DARK_GREEN}%s{YELLOW} created."), groupName);
        }
        else {
            sendMessage(sender, colorize("{RED}Group {DARK_GREEN}%s{RED} already exists."), groupName);
            abortBatchProcessing();
        }
    }

    @Command(value="add", description="Add a player to a group")
    @Require("zpermissions.group.manage")
    public void addMember(CommandSender sender, @Session("entityName") String groupName, @Option({"-a", "--add"}) boolean add, @Option({"-A", "--add-no-reset"}) boolean addNoReset, @Option(value="player", completer="player") String playerName, @Option(value="duration/timestamp", optional=true) String duration, String[] args) {
        addGroupMember(sender, groupName, playerName, duration, args, add, addNoReset);
    }

    @Command(value={"remove", "rm"}, description="Remove a player from a group")
    @Require("zpermissions.group.manage")
    public void removeMember(CommandSender sender, @Session("entityName") String groupName, @Option(value="player", completer="player") String playerName) {
        removeGroupMember(sender, groupName, playerName);
    }

    @Command(value={"show", "sh"}, description="Show information about a group")
    @Require("zpermissions.group.view")
    public void show(CommandSender sender, @Session("entityName") String groupName, @Option(value={"-f", "--filter"}, valueName="filter") String filter) {
        PermissionEntity entity = storageStrategy.getPermissionService().getEntity(groupName, null, true);

        if (entity != null) {
            List<String> lines = new ArrayList<>();
            lines.add(String.format(colorize("{YELLOW}Declared permissions for {DARK_GREEN}%s{YELLOW}:"), entity.getDisplayName()));
            lines.add(String.format(colorize("{YELLOW}Weight: {GREEN}%s"), entity.getPriority()));
            List<PermissionEntity> parents = entity.getParents();
            if (!parents.isEmpty()) {
                List<String> parentNames = new ArrayList<>(parents.size());
                for (PermissionEntity parent : parents)
                    parentNames.add(parent.getDisplayName());
                lines.add(String.format(colorize("{YELLOW}Parent%s: {DARK_GREEN}%s"),
                        (parentNames.size() == 1 ? "" : "s"),
                        ToHStringUtils.delimitedString(", ", parentNames)));
            }
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

        if (entity == null) {
            sendMessage(sender, colorize("{RED}Group {DARK_GREEN}%s{RED} does not exist."), groupName);
            abortBatchProcessing();
            return;
        }
        else if (entity.getPermissions().isEmpty()) {
            sendMessage(sender, colorize("{RED}Group has no declared permissions."));
            return;
        }
    }

    @Command(value={"setparents", "parents", "setparent", "parent"}, description="Set a group's parent(s)",
            completer="group", varargs="parent...")
    @Require("zpermissions.group.manage")
    public void setParent(CommandSender sender, final @Session("entityName") String groupName, String[] parents) {
        final List<String> parentNames = Arrays.asList(parents);
        try {
            // Set parent. Creates group and/or parent if missing.
            storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
                @Override
                public void doInTransactionWithoutResult() throws Exception {
                    storageStrategy.getPermissionService().setParents(groupName, parentNames);
                }
            });
        }
        catch (MissingGroupException e) {
            handleMissingGroup(sender, e);
            return;
        }
        catch (PermissionServiceException e) {
            // Most likely due to inheritance cycle
            sendMessage(sender, colorize("{RED}%s"), e.getMessage());
            abortBatchProcessing();
            return;
        }

        if (parentNames.isEmpty())
            sendMessage(sender, colorize("{DARK_GREEN}%s{YELLOW} now has no parents"), groupName);
        else if (parentNames.size() == 1)
            sendMessage(sender, colorize("{DARK_GREEN}%s{YELLOW}'s parent is now {DARK_GREEN}%s"), groupName, parentNames.get(0));
        else
            sendMessage(sender, colorize("{DARK_GREEN}%s{YELLOW}'s parents are now {DARK_GREEN}%s"), groupName, delimitedString(ChatColor.YELLOW + ", " + ChatColor.DARK_GREEN, parentNames));
        core.invalidateMetadataCache(groupName, null, true);
        core.refreshAffectedPlayers(groupName);
    }

    @Command(value={"setweight", "weight", "setpriority", "priority"}, description="Set a group's weight")
    @Require("zpermissions.group.manage")
    public void setPriority(CommandSender sender, final @Session("entityName") String groupName, final @Option("weight") int priority) {
        // Set the priority. Will not fail, creates group if necessary
        try {
            storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
                @Override
                public void doInTransactionWithoutResult() throws Exception {
                    storageStrategy.getPermissionService().setPriority(groupName, priority);
                }
            });
        }
        catch (MissingGroupException e) {
            handleMissingGroup(sender, e);
            return;
        }

        sendMessage(sender, colorize("{DARK_GREEN}%s{YELLOW}'s weight is now {GREEN}%d"), groupName, priority);
        core.invalidateMetadataCache(groupName, null, true);
        core.refreshAffectedPlayers(groupName);
    }

    @Command(value="members", description="List members of a group")
    @Require("zpermissions.group.view")
    public void members(CommandSender sender, @Option(value={"-U", "--uuid"}) boolean showUuid, @Session("entityName") String groupName) {
        List<Membership> memberships = storageStrategy.getPermissionService().getMembers(groupName);
        
        // NB: Can't tell if group doesn't exist or if it has no members.
        if (memberships.isEmpty()) {
            sendMessage(sender, colorize("{YELLOW}Group has no members or does not exist."));
        }
        else {
            Date now = new Date();
            StringBuilder sb = new StringBuilder();
            for (Iterator<Membership> i = memberships.iterator(); i.hasNext();) {
                Membership membership = i.next();
                if (membership.getExpiration() == null || membership.getExpiration().after(now))
                    sb.append(ChatColor.AQUA);
                else
                    sb.append(ChatColor.GRAY);

                sb.append(formatPlayerName(membership, showUuid));

                if (membership.getExpiration() != null) {
                    sb.append('[');
                    sb.append(Utils.dateToString(membership.getExpiration()));
                    sb.append(']');
                }

                if (i.hasNext()) {
                    sb.append(ChatColor.YELLOW);
                    sb.append(", ");
                }
            }
            sendMessage(sender, colorize("{YELLOW}Members of {DARK_GREEN}%s{YELLOW}: %s"), groupName, sb);
        }
    }

    @Command(value={"clone", "copy", "cp"}, description="Clone this group")
    @Require("zpermissions.group.manage")
    public void clone(CommandSender sender, @Session("entityName") String groupName, @Option("new-group") String destination) {
        super.clone(sender, groupName, destination, false);
    }

    @Command(value={"rename", "ren", "mv"}, description="Rename this group")
    @Require("zpermissions.group.manage")
    public void rename(CommandSender sender, @Session("entityName") String groupName, @Option("new-group") String destination) {
        super.clone(sender, groupName, destination, true);
    }

}
