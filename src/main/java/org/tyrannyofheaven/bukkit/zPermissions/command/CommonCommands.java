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
import static org.tyrannyofheaven.bukkit.util.command.reader.CommandReader.abortBatchProcessing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.util.command.HelpBuilder;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.util.uuid.CommandUuidResolver;
import org.tyrannyofheaven.bukkit.util.uuid.CommandUuidResolverHandler;
import org.tyrannyofheaven.bukkit.zPermissions.PermissionsResolver;
import org.tyrannyofheaven.bukkit.zPermissions.QualifiedPermission;
import org.tyrannyofheaven.bukkit.zPermissions.RefreshCause;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsConfig;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsCore;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsPlugin;
import org.tyrannyofheaven.bukkit.zPermissions.dao.MissingGroupException;
import org.tyrannyofheaven.bukkit.zPermissions.model.EntityMetadata;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.storage.StorageStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.util.MetadataConstants;
import org.tyrannyofheaven.bukkit.zPermissions.util.Utils;

/**
 * Handler for common commands between "/permissions group" and
 * "/permissions player"
 * 
 * @author zerothangel
 */
public abstract class CommonCommands {

    protected final ZPermissionsCore core;

    protected final StorageStrategy storageStrategy;

    protected final PermissionsResolver resolver;

    protected final ZPermissionsConfig config;

    // Parent plugin
    protected final Plugin plugin;

    protected final CommandUuidResolver uuidResolver;

    // true if this is handling groups
    private final boolean group;

    private final MetadataCommands metadataCommands;

    /**
     * Instantiate this handler.
     * @param group true if this is handling groups
     */
    protected CommonCommands(ZPermissionsCore core, StorageStrategy storageStrategy, PermissionsResolver resolver, ZPermissionsConfig config, Plugin plugin, CommandUuidResolver uuidResolver, boolean group) {
        this.core = core;
        this.storageStrategy = storageStrategy;
        this.resolver = resolver;
        this.config = config;
        this.plugin = plugin;
        this.uuidResolver = uuidResolver;
        this.group = group;
        
        metadataCommands = new MetadataCommands(core, storageStrategy, uuidResolver, group);
    }

    protected final MetadataCommands getMetadataCommands() {
        return metadataCommands;
    }

    protected final void _get(CommandSender sender, final String name, final String permission) {
        uuidResolver.resolveUsername(sender, name, group, new CommandUuidResolverHandler() {
            @Override
            public void process(CommandSender sender, String name, UUID uuid, boolean group) {
                _get0(sender, name, uuid, permission);
            }
        });
    }

    private void _get0(CommandSender sender, final String name, final UUID uuid, String permission) {
        // Get world/permission
        final QualifiedPermission wp = new QualifiedPermission(permission);

        // Don't allow messing with the dynamic permission
        if (checkDynamicPermission(sender, wp.getPermission())) return;

        // Read entry from PermissionService, if any
        Boolean result = storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                return storageStrategy.getPermissionService().getPermission(name, uuid, group, wp.getRegion(), wp.getWorld(), wp.getPermission());
            }
        }, true);
        
        if (result == null) {
            sendMessage(sender, colorize("%s%s{YELLOW} does not set {GOLD}%s"), group ? ChatColor.DARK_GREEN : ChatColor.AQUA, name, permission);
            abortBatchProcessing();
        }
        else {
            sendMessage(sender, colorize("%s%s{YELLOW} sets {GOLD}%s{YELLOW} to {GREEN}%s"), group ? ChatColor.DARK_GREEN : ChatColor.AQUA, name, permission, result);
        }
    }

    protected final void _set(CommandSender sender, final String name, final String permission, final Boolean value) {
        uuidResolver.resolveUsername(sender, name, group, new CommandUuidResolverHandler() {
            @Override
            public void process(CommandSender sender, String name, UUID uuid, boolean group) {
                _set0(sender, name, uuid, permission, value);
            }
        });
    }

    private void _set0(CommandSender sender, final String name, final UUID uuid, String permission, final Boolean value) {
        // Get world/permission
        final QualifiedPermission wp = new QualifiedPermission(permission);
    
        // Don't allow messing with the dynamic permission
        if (checkDynamicPermission(sender, wp.getPermission())) return;

        // Set permission.
        try {
            storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
                @Override
                public void doInTransactionWithoutResult() throws Exception {
                    storageStrategy.getPermissionService().setPermission(name, uuid, group, wp.getRegion(), wp.getWorld(), wp.getPermission(), value == null ? Boolean.TRUE : value);
                }
            });
        }
        catch (MissingGroupException e) {
            handleMissingGroup(sender, e);
            return;
        }
    
        sendMessage(sender, colorize("{GOLD}%s{YELLOW} set to {GREEN}%s{YELLOW} for %s%s"), permission, value == null ? Boolean.TRUE : value, group ? ChatColor.DARK_GREEN : ChatColor.AQUA, name);
        if (!group) {
            core.refreshPlayer(uuid, RefreshCause.COMMAND);
        }
        else {
            core.refreshAffectedPlayers(name);
        }
    }

    protected final void _unset(CommandSender sender, final String name, final String permission) {
        uuidResolver.resolveUsername(sender, name, group, new CommandUuidResolverHandler() {
            @Override
            public void process(CommandSender sender, String name, UUID uuid, boolean group) {
                _unset0(sender, name, uuid, permission);
            }
        });
    }

    private void _unset0(CommandSender sender, final String name, final UUID uuid, String permission) {
        // Get world/permission
        final QualifiedPermission wp = new QualifiedPermission(permission);
    
        // Don't allow messing with the dynamic permission
        if (checkDynamicPermission(sender, wp.getPermission())) return;

        // Delete permission entry.
        Boolean result = storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                return storageStrategy.getPermissionService().unsetPermission(name, uuid, group, wp.getRegion(), wp.getWorld(), wp.getPermission());
            }
        });

        if (result) {
            sendMessage(sender, colorize("{GOLD}%s{YELLOW} unset for %s%s"), permission, group ? ChatColor.DARK_GREEN : ChatColor.AQUA, name);
            if (group)
                core.refreshAffectedPlayers(name);
            else
                core.refreshPlayer(uuid, RefreshCause.COMMAND);
        }
        else {
            sendMessage(sender, colorize("%s%s{RED} does not set {GOLD}%s"), group ? ChatColor.DARK_GREEN : ChatColor.AQUA, name, permission);
            abortBatchProcessing();
        }
    }

    /**
     * Verifies given permission does not start with the dynamic permission prefix.
     * 
     * @param sender the CommandSender to complain to if it does
     * @param permission the permission (must be unqualified)
     * @return true if it starts with the prefix, false otherwise
     */
    protected final boolean checkDynamicPermission(CommandSender sender, String permission) {
        permission = permission.toLowerCase();
        if (permission.startsWith(ZPermissionsPlugin.DYNAMIC_PERMISSION_PREFIX.toLowerCase())) {
            sendMessage(sender, colorize("{RED}I don't think so."));
            return true;
        }
        return false;
    }

    protected final void _delete(CommandSender sender, final String name) {
        uuidResolver.resolveUsername(sender, name, group, new CommandUuidResolverHandler() {
            @Override
            public void process(CommandSender sender, String name, UUID uuid, boolean group) {
                _delete0(sender, name, uuid);
            }
        });
    }

    private void _delete0(CommandSender sender, final String name, final UUID uuid) {
        boolean result = storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                return storageStrategy.getPermissionService().deleteEntity(name, uuid, group);
            }
        });
        
        if (result) {
            sendMessage(sender, colorize("{YELLOW}%s %s%s{YELLOW} deleted"),
                    (group ? "Group" : "Player"),
                    (group ? ChatColor.DARK_GREEN : ChatColor.AQUA),
                    name);
            core.invalidateMetadataCache(name, uuid, group);
            if (group)
                core.refreshAffectedPlayers(name);
            else
                core.refreshPlayer(uuid, RefreshCause.COMMAND);
            core.refreshExpirations();
        }
        else {
            sendMessage(sender, colorize("{RED}%s not found."), group ? "Group" : "Player");
            abortBatchProcessing();
        }
    }

    protected final void _dump(CommandSender sender, final String name, final String worldName, final String filter, final String[] regionNames) {
        uuidResolver.resolveUsername(sender, name, group, new CommandUuidResolverHandler() {
            @Override
            public void process(CommandSender sender, String name, UUID uuid, boolean group) {
                _dump0(sender, name, uuid, worldName, filter, regionNames);
            }
        });
    }

    private void _dump0(CommandSender sender, final String name, final UUID uuid, String worldName, String filter, String[] regionNames) {
        List<String> header = new ArrayList<>();
        worldName = getEffectiveWorld(sender, worldName, header);
        if (worldName == null) return;

        if (!group)
            Utils.validatePlayer(storageStrategy.getPermissionService(), resolver.getDefaultGroup(), uuid, name, header);

        // Ensure regions are lowercased
        final Set<String> regions = new LinkedHashSet<>();
        for (String region : regionNames) {
            regions.add(region.toLowerCase());
        }
        
        final String lworldName = worldName.toLowerCase();
        Map<String, Boolean> rootPermissions;
        try {
            // Grab permissions from zPerms
            rootPermissions = storageStrategy.getTransactionStrategy().execute(new TransactionCallback<Map<String, Boolean>>() {
                @Override
                public Map<String, Boolean> doInTransaction() throws Exception {
                    if (group) {
                        if (storageStrategy.getPermissionService().getEntity(name, null, true) == null)
                            throw new MissingGroupException(name); // Don't really want to handle it in the transaction...
                        return resolver.resolveGroup(name.toLowerCase(), lworldName, regions);
                    }
                    else {
                        return resolver.resolvePlayer(uuid, lworldName, regions).getPermissions();
                    }
                }
            }, true);
        }
        catch (MissingGroupException e) {
            handleMissingGroup(sender, e);
            return;
        }

        // Recursively determine all child permissions
        Map<String, Boolean> permissions = new HashMap<>();
        Utils.calculateChildPermissions(permissions, rootPermissions, false);
        
        Utils.displayPermissions(plugin, sender, header, permissions, filter);
    }

    // Given a world name (which may be null), get the world to use for effective permissions calculation
    private String getEffectiveWorld(CommandSender sender, String worldName, List<String> header) {
        if (worldName == null) {
            // Determine a default world
            if (sender instanceof Player) {
                worldName = ((Player)sender).getWorld().getName();
                header.add(String.format(colorize("{GRAY}(Using current world: %s. Use -w to specify a world.)"), worldName));
            }
            else {
                List<World> worlds = Bukkit.getWorlds();
                if (!worlds.isEmpty()) {
                    worldName = worlds.get(0).getName();
                    header.add(String.format(colorize("{GRAY}(Use -w to specify a world. Defaulting to \"%s\")"), worldName));
                }
            }
        }
        else {
            // Validate world name
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                sendMessage(sender, colorize("{RED}Invalid world."));
                return null;
            }
        }
        return worldName;
    }

    protected final void _diff(CommandSender sender, final String name, final String worldName, final String filter, final String otherName, final String[] regionNames) {
        final CommonCommands realThis = this;
        uuidResolver.resolveUsername(sender, name, group, new CommandUuidResolverHandler() {
            @Override
            public void process(CommandSender sender, final String name, final UUID uuid, boolean group) {
                // Resolve both names...
                uuidResolver.resolveUsername(sender, otherName, group, new CommandUuidResolverHandler() {
                    @Override
                    public void process(CommandSender sender, String otherName, UUID otherUuid, boolean group) {
                        realThis._diff(sender, name, uuid, worldName, filter, otherName, otherUuid, regionNames);
                    }
                });
            }
        });
    }

    private void _diff(CommandSender sender, final String name, final UUID uuid, String worldName, String filter, final String otherName, final UUID otherUuid, String[] regionNames) {
        List<String> header = new ArrayList<>();
        worldName = getEffectiveWorld(sender, worldName, header);
        if (worldName == null) return;

        if (!group) {
            Utils.validatePlayer(storageStrategy.getPermissionService(), resolver.getDefaultGroup(), uuid, name, header);
            Utils.validatePlayer(storageStrategy.getPermissionService(), resolver.getDefaultGroup(), otherUuid, otherName, header);
        }

        // Ensure regions are lowercased
        final Set<String> regions = new LinkedHashSet<>();
        for (String region : regionNames) {
            regions.add(region.toLowerCase());
        }
        
        // Grab permissions from zPerms
        final String lworldName = worldName.toLowerCase();
        Map<String, Boolean> rootPermissions;
        try {
            rootPermissions = storageStrategy.getTransactionStrategy().execute(new TransactionCallback<Map<String, Boolean>>() {
                @Override
                public Map<String, Boolean> doInTransaction() throws Exception {
                    if (group) {
                        if (storageStrategy.getPermissionService().getEntity(name, null, true) == null)
                            throw new MissingGroupException(name); // Don't really want to handle it in the transaction...
                        return resolver.resolveGroup(name.toLowerCase(), lworldName, regions);
                    }
                    else {
                        return resolver.resolvePlayer(uuid, lworldName, regions).getPermissions();
                    }
                }
            }, true);
        }
        catch (MissingGroupException e) {
            handleMissingGroup(sender, e);
            return;
        }

        // Recursively determine all child permissions
        Map<String, Boolean> permissions = new HashMap<>();
        Utils.calculateChildPermissions(permissions, rootPermissions, false);

        // Grab permissions of other entity
        Map<String, Boolean> otherRootPermissions;
        try {
            otherRootPermissions = storageStrategy.getTransactionStrategy().execute(new TransactionCallback<Map<String, Boolean>>() {
                @Override
                public Map<String, Boolean> doInTransaction() throws Exception {
                    if (group) {
                        if (storageStrategy.getPermissionService().getEntity(otherName, null, true) == null)
                            throw new MissingGroupException(otherName); // Don't really want to handle it in the transaction...
                        return resolver.resolveGroup(otherName.toLowerCase(), lworldName, regions);
                    }
                    else {
                        return resolver.resolvePlayer(otherUuid, lworldName, regions).getPermissions();
                    }
                }
            }, true);
        }
        catch (MissingGroupException e) {
            handleMissingGroup(sender, e);
            return;
        }

        Map<String, Boolean> otherPermissions = new HashMap<>();
        Utils.calculateChildPermissions(otherPermissions, otherRootPermissions, false);
        
        Utils.displayPermissionsDiff(plugin, sender, permissions, otherPermissions, header,
                String.format(colorize("%s%s {WHITE}adds {YELLOW}the following permissions:"),
                        (group ? ChatColor.DARK_GREEN : ChatColor.AQUA),
                        otherName),
                String.format(colorize("%s%s {WHITE}removes {YELLOW}the following permissions:"),
                        (group ? ChatColor.DARK_GREEN : ChatColor.AQUA),
                        otherName),
                String.format(colorize("%s%s {WHITE}changes {YELLOW}the following permissions:"),
                        (group ? ChatColor.DARK_GREEN : ChatColor.AQUA),
                        otherName),
                String.format(colorize("{YELLOW}%ss have identical effective permissions."), group ? "Group" : "Player"), filter);
    }

    protected final MetadataCommands _metadata(HelpBuilder helpBuilder, CommandSender sender, String[] args) {
        if (args.length == 0) {
            helpBuilder.withCommandSender(sender)
                .withHandler(metadataCommands)
                .forCommand("get")
                .forCommand("set")
                .forCommand("setint")
                .forCommand("setreal")
                .forCommand("setbool")
                .forCommand("unset")
                .forCommand("show")
                .show();
            abortBatchProcessing();
            return null;
        }
        return metadataCommands;
    }

    protected final void clone(final CommandSender sender, final String name, final String destination, final boolean rename) {
        // Don't allow rename of players
        if (!group && rename)
            throw new IllegalArgumentException();

        final CommonCommands realThis = this;
        uuidResolver.resolveUsername(sender, name, group, new CommandUuidResolverHandler() {
            @Override
            public void process(CommandSender sender, final String sourceName, final UUID sourceUuid, boolean group) {
                // Resolve twice (yeah, pretty insane...)
                uuidResolver.resolveUsername(sender, destination, group, new CommandUuidResolverHandler() {
                    @Override
                    public void process(CommandSender sender, String destination, UUID destinationUuid, boolean group) {
                        realThis.clone(sender, sourceName, sourceUuid, destination, destinationUuid, rename);
                    }
                });
            }
        });
    }

    private void clone(final CommandSender sender, final String name, final UUID sourceUuid, final String destination, final UUID destinationUuid, final boolean rename) {
        storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                PermissionEntity entity = storageStrategy.getPermissionService().getEntity(name, sourceUuid, group);
                List<Membership> memberships = Collections.emptyList();
                if (!group) {
                    memberships = storageStrategy.getPermissionService().getGroups(sourceUuid);
                }
                if (entity == null && memberships.isEmpty()) {
                    // Nothing to copy
                    sendMessage(sender, colorize("{RED}%s %s%s{RED} does not exist."),
                            (group ? "Group" : "Player"),
                            (group ? ChatColor.DARK_GREEN : ChatColor.AQUA),
                            name);
                    abortBatchProcessing();
                    return;
                }
                if (storageStrategy.getPermissionService().getEntity(destination, destinationUuid, group) != null) {
                    sendMessage(sender, colorize("{RED}%s %s%s{RED} already exists. Purge it if you really want to overwrite."),
                            (group ? "Group" : "Player"),
                            (group ? ChatColor.DARK_GREEN : ChatColor.AQUA),
                            destination);
                    abortBatchProcessing();
                    return;
                }
                
                // Create if group
                if (group) {
                    storageStrategy.getPermissionService().createGroup(destination);
                }
                if (entity != null) {
                    // Clone permissions
                    for (Entry entry : entity.getPermissions()) {
                        storageStrategy.getPermissionService().setPermission(destination, destinationUuid,
                                group,
                                (entry.getRegion() != null ? entry.getRegion().getName() : null),
                                (entry.getWorld() != null ? entry.getWorld().getName() : null), entry.getPermission(), entry.isValue());
                    }
                    // Clone metadata
                    for (EntityMetadata metadata : entity.getMetadata()) {
                        storageStrategy.getPermissionService().setMetadata(destination, destinationUuid, group, metadata.getName(), metadata.getValue());
                    }
                }
                if (group) {
                    // Group-specific stuff
                    storageStrategy.getPermissionService().setPriority(destination, entity.getPriority());
                    List<String> parentNames = new ArrayList<>();
                    for (PermissionEntity parent : entity.getParents())
                        parentNames.add(parent.getDisplayName());
                    storageStrategy.getPermissionService().setParents(destination, parentNames);
                }
                else {
                    // Player-specific stuff
                    for (Membership membership : memberships) {
                        storageStrategy.getPermissionService().addMember(membership.getGroup().getDisplayName(), destinationUuid, destination, membership.getExpiration());
                    }
                }

                if (rename) {
                    if (group) {
                        // Move child groups to destination
                        for (PermissionEntity child : entity.getChildrenNew()) {
                            List<String> newParents = new ArrayList<>();
                            for (PermissionEntity parent : child.getParents()) {
                                if (parent.equals(entity))
                                    newParents.add(destination);
                                else
                                    newParents.add(parent.getDisplayName());
                            }
                            storageStrategy.getPermissionService().setParents(child.getDisplayName(), newParents);
                        }
                        
                        // Add players to destination
                        for (Membership membership : entity.getMemberships()) {
                            storageStrategy.getPermissionService().addMember(destination, membership.getUuid(), membership.getDisplayName(), membership.getExpiration());
                        }
                    }
                    
                    // NB Nothing more to do for players
                    
                    // Delete original
                    storageStrategy.getPermissionService().deleteEntity(name, sourceUuid, group);
                }

                broadcastAdmin(plugin, "%s %s %s %s to %s", sender.getName(),
                        (rename ? "renamed" : "cloned"),
                        (group ? "group" : "player"),
                        name, destination);
                sendMessage(sender, colorize("{YELLOW}%s %s%s{YELLOW} %s to %s%s{YELLOW}."),
                        (group ? "Group" : "Player"),
                        (group ? ChatColor.DARK_GREEN : ChatColor.AQUA),
                        name,
                        (rename ? "renamed" : "cloned"),
                        (group ? ChatColor.DARK_GREEN : ChatColor.AQUA),
                        destination);
            }
        });

        // Don't bother trying to selectively refresh
        // Redo ALL THE THINGS!
        core.invalidateMetadataCache();
        core.refreshPlayers();
        core.refreshExpirations();
    }

    protected final void _prefix(CommandSender sender, String name, String prefix, String[] rest) {
        if ((prefix != null && !prefix.isEmpty()) || rest.length > 0) {
            metadataCommands.set(sender, name, MetadataConstants.PREFIX_KEY, prefix, rest);
        }
        else {
            metadataCommands.unset(sender, name, MetadataConstants.PREFIX_KEY);
        }
    }

    protected final void _suffix(CommandSender sender, String name, String suffix, String[] rest) {
        if ((suffix != null && !suffix.isEmpty()) || rest.length > 0) {
            metadataCommands.set(sender, name, MetadataConstants.SUFFIX_KEY, suffix, rest);
        }
        else {
            metadataCommands.unset(sender, name, MetadataConstants.SUFFIX_KEY);
        }
    }

    protected final String formatEntry(CommandSender sender, Entry e) {
        return String.format(colorize("{DARK_GREEN}- {GOLD}%s%s%s{DARK_GREEN}: {GREEN}%s"),
                (e.getRegion() == null ? "" : e.getRegion().getName() + colorize("{DARK_GREEN}/{GOLD}")),
                (e.getWorld() == null ? "" : e.getWorld().getName() + colorize("{DARK_GREEN}:{GOLD}")),
                e.getPermission(),
                e.isValue());
    }

    protected final void handleMissingGroup(CommandSender sender, MissingGroupException e) {
        sendMessage(sender, colorize("{RED}Group {DARK_GREEN}%s{RED} does not exist."), e.getGroupName());
        abortBatchProcessing();
    }

    protected final void addGroupMember(CommandSender sender, final String groupName, final String playerName, final String duration, final String[] args, final boolean add, final boolean addNoReset) {
        uuidResolver.resolveUsername(sender, playerName, false, new CommandUuidResolverHandler() {
            @Override
            public void process(CommandSender sender, String name, UUID uuid, boolean group) {
                addGroupMember(sender, groupName, uuid, name, duration, args, add, addNoReset);
            }
        });
    }

    private void addGroupMember(CommandSender sender, final String groupName, final UUID playerUuid, final String playerName, String duration, String[] args, final boolean add, final boolean addNoReset) {
        final Date expiration = Utils.parseDurationTimestamp(duration, args);
    
        // Add player to group.
        try {
            storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
                @Override
                public void doInTransactionWithoutResult() throws Exception {
                    Date newExpiration = handleExtendExpiration(groupName, playerUuid, playerName, add, addNoReset, expiration);

                    storageStrategy.getPermissionService().addMember(groupName, playerUuid, playerName, newExpiration);
                }
            });
        }
        catch (MissingGroupException e) {
            handleMissingGroup(sender, e);
            return;
        }
    
        sendMessage(sender, colorize("{AQUA}%s{YELLOW} added to {DARK_GREEN}%s"), playerName, groupName);
        core.invalidateMetadataCache(playerName, playerUuid, false);
        core.refreshPlayer(playerUuid, RefreshCause.GROUP_CHANGE);
        
        if (expiration != null)
            core.refreshExpirations(playerUuid);
    }

    protected final void removeGroupMember(CommandSender sender, final String groupName, final String playerName) {
        uuidResolver.resolveUsername(sender, playerName, false, new CommandUuidResolverHandler() {
            @Override
            public void process(CommandSender sender, String name, UUID uuid, boolean group) {
                removeGroupMember(sender, groupName, uuid, name);
            }
        });
    }

    private void removeGroupMember(CommandSender sender, final String groupName, final UUID playerUuid, final String playerName) {
        // Remove player from group
        Boolean result = storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                return storageStrategy.getPermissionService().removeMember(groupName, playerUuid);
            }
        });
    
        if (result) {
            sendMessage(sender, colorize("{AQUA}%s{YELLOW} removed from {DARK_GREEN}%s"), playerName, groupName);
            core.invalidateMetadataCache(playerName, playerUuid, false);
            core.refreshPlayer(playerUuid, RefreshCause.GROUP_CHANGE);
            core.refreshExpirations(playerUuid);
        }
        else {
            sendMessage(sender, colorize("{DARK_GREEN}%s{RED} does not exist or {AQUA}%s{RED} is not a member"), groupName, playerName);
            abortBatchProcessing();
        }
    }

    protected final Date handleExtendExpiration(final String groupName, UUID playerUuid, final String playerName, final boolean add, boolean addNoReset, final Date expiration) {
        Date newExpiration = expiration;
        if (add || addNoReset) {
            if (expiration != null) {
                Date now = new Date();

                List<Membership> memberships = storageStrategy.getPermissionService().getGroups(playerUuid);

                // Determine a previous duration, if any
                long previousDuration = 0L;
                for (Membership membership : memberships) {
                    if (membership.getGroup().getName().equalsIgnoreCase(groupName)) {
                        if (membership.getExpiration() != null) {
                            previousDuration = membership.getExpiration().getTime() - now.getTime();
                            // Sanity
                            if (previousDuration < 0L)
                                previousDuration = 0L;
                        }
                        else if (addNoReset) return null; // Exists and is permanent, don't touch expiration if requested
                        break;
                    }
                }

                long newDuration = expiration.getTime() - now.getTime();
                if (newDuration < 0L)
                    newDuration = 0L;
                
                // Set new expiration
                newExpiration = new Date(now.getTime() + previousDuration + newDuration);
            }
        }
        return newExpiration;
    }

}
