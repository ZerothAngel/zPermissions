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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.util.ToHStringUtils;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.HelpBuilder;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Session;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.zPermissions.PermissionsResolver;
import org.tyrannyofheaven.bukkit.zPermissions.QualifiedPermission;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsConfig;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsCore;
import org.tyrannyofheaven.bukkit.zPermissions.dao.MissingGroupException;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.storage.StorageStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.util.MetadataConstants;
import org.tyrannyofheaven.bukkit.zPermissions.util.Utils;

/**
 * Handler for common commands between "/permissions group" and
 * "/permissions player"
 * 
 * @author asaddi
 */
public abstract class CommonCommands {

    protected final ZPermissionsCore core;

    protected final StorageStrategy storageStrategy;

    protected final PermissionsResolver resolver;

    protected final ZPermissionsConfig config;

    // Parent plugin
    protected final Plugin plugin;

    // true if this is handling groups
    private final boolean group;

    private final MetadataCommands metadataCommands;

    /**
     * Instantiate this handler.
     * @param group true if this is handling groups
     */
    protected CommonCommands(ZPermissionsCore core, StorageStrategy storageStrategy, PermissionsResolver resolver, ZPermissionsConfig config, Plugin plugin, boolean group) {
        this.core = core;
        this.storageStrategy = storageStrategy;
        this.resolver = resolver;
        this.config = config;
        this.plugin = plugin;
        this.group = group;
        
        metadataCommands = new MetadataCommands(storageStrategy, group);
    }

    protected MetadataCommands getMetadataCommands() {
        return metadataCommands;
    }

    @Command(value="get", description="View a permission")
    public void get(CommandSender sender, final @Session("entityName") String name, @Option("permission") String permission) {
        // Get world/permission
        final QualifiedPermission wp = new QualifiedPermission(permission);

        // Read entry from DAO, if any
        Boolean result = storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                return storageStrategy.getDao().getPermission(name, group, wp.getRegion(), wp.getWorld(), wp.getPermission());
            }
        });
        
        if (result == null) {
            sendMessage(sender, colorize("%s%s{YELLOW} does not set {GOLD}%s"), group ? ChatColor.DARK_GREEN : ChatColor.AQUA, name, permission);
            if (!group)
                Utils.checkPlayer(sender, name);
            abortBatchProcessing();
        }
        else {
            sendMessage(sender, colorize("%s%s{YELLOW} sets {GOLD}%s{YELLOW} to {GREEN}%s"), group ? ChatColor.DARK_GREEN : ChatColor.AQUA, name, permission, result);
        }
    }

    @Command(value="set", description="Set a permission")
    public void set(CommandSender sender, final @Session("entityName") String name, @Option("permission") String permission, final @Option(value="value", optional=true) Boolean value) {
        // Get world/permission
        final QualifiedPermission wp = new QualifiedPermission(permission);
    
        // Set permission.
        try {
            storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
                @Override
                public void doInTransactionWithoutResult() throws Exception {
                    storageStrategy.getDao().setPermission(name, group, wp.getRegion(), wp.getWorld(), wp.getPermission(), value == null ? Boolean.TRUE : value);
                }
            });
        }
        catch (MissingGroupException e) {
            handleMissingGroup(sender, e);
            return;
        }
    
        sendMessage(sender, colorize("{GOLD}%s{YELLOW} set to {GREEN}%s{YELLOW} for %s%s"), permission, value == null ? Boolean.TRUE : value, group ? ChatColor.DARK_GREEN : ChatColor.AQUA, name);
        if (!group) {
            Utils.checkPlayer(sender, name);
            core.refreshPlayer(name);
        }
        else {
            core.refreshAffectedPlayers(name);
        }
    }

    @Command(value="unset", description="Remove a permission")
    public void unset(CommandSender sender, final @Session("entityName") String name, @Option("permission") String permission) {
        // Get world/permission
        final QualifiedPermission wp = new QualifiedPermission(permission);
    
        // Delete permission entry.
        Boolean result = storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                return storageStrategy.getDao().unsetPermission(name, group, wp.getRegion(), wp.getWorld(), wp.getPermission());
            }
        });

        if (result) {
            sendMessage(sender, colorize("{GOLD}%s{YELLOW} unset for %s%s"), permission, group ? ChatColor.DARK_GREEN : ChatColor.AQUA, name);
            if (group)
                core.refreshAffectedPlayers(name);
            else
                core.refreshPlayer(name);
        }
        else {
            sendMessage(sender, colorize("%s%s{RED} does not set {GOLD}%s"), group ? ChatColor.DARK_GREEN : ChatColor.AQUA, name, permission);
            if (!group)
                Utils.checkPlayer(sender, name);
            abortBatchProcessing();
        }
    }

    @Command(value="purge", description="Delete this group or player") // doh!
    public void delete(CommandSender sender, final @Session("entityName") String name) {
        boolean result = storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                return storageStrategy.getDao().deleteEntity(name, group);
            }
        });
        
        if (result) {
            sendMessage(sender, colorize("{YELLOW}%s %s%s{YELLOW} deleted"),
                    (group ? "Group" : "Player"),
                    (group ? ChatColor.DARK_GREEN : ChatColor.AQUA),
                    name);
            if (group)
                core.refreshAffectedPlayers(name);
            else
                core.refreshPlayer(name);
            core.refreshExpirations();
        }
        else {
            sendMessage(sender, colorize("{RED}%s not found."), group ? "Group" : "Player");
            abortBatchProcessing();
        }
    }

    @Command(value="dump", description="Display permissions for this group or player", varargs="region...")
    public void dump(CommandSender sender, final @Session("entityName") String name, @Option(value={"-w", "--world"}, valueName="world", completer="world") String worldName, @Option(value={"-f", "--filter"}, valueName="filter") String filter, String[] regionNames) {
        List<String> header = new ArrayList<String>();
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
                return;
            }
        }

        // Ensure regions are lowercased
        final Set<String> regions = new HashSet<String>();
        for (String region : regionNames) {
            regions.add(region.toLowerCase());
        }
        
        // Grab permissions from zPerms
        final String lworldName = worldName.toLowerCase();
        Map<String, Boolean> rootPermissions = storageStrategy.getTransactionStrategy().execute(new TransactionCallback<Map<String, Boolean>>() {
            @Override
            public Map<String, Boolean> doInTransaction() throws Exception {
                if (group) {
                    return resolver.resolveGroup(name.toLowerCase(), lworldName, regions);
                }
                else {
                    return resolver.resolvePlayer(name.toLowerCase(), lworldName, regions).getPermissions();
                }
            }
        });

        // Recursively determine all child permissions
        Map<String, Boolean> permissions = new HashMap<String, Boolean>();
        calculateChildPermissions(permissions, rootPermissions, false);
        
        Utils.displayPermissions(plugin, sender, header, permissions, filter);
    }

    @Command(value={"metadata", "meta", "md"}, description="Metadata-related commands")
    public MetadataCommands metadata(HelpBuilder helpBuilder, CommandSender sender, String[] args) {
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
            return null;
        }
        return metadataCommands;
    }

    @Command(value="prefix", description="Set chat prefix for this group or player")
    public void prefix(CommandSender sender, @Session("entityName") String name, @Option(value="prefix", optional=true) String prefix, String[] rest) {
        if (ToHStringUtils.hasText(prefix)) {
            metadataCommands.set(sender, name, MetadataConstants.PREFIX_KEY, prefix, rest);
        }
        else {
            metadataCommands.unset(sender, name, MetadataConstants.PREFIX_KEY);
        }
    }

    @Command(value="suffix", description="Set chat suffix for this group or player")
    public void suffix(CommandSender sender, @Session("entityName") String name, @Option(value="suffix", optional=true) String suffix, String[] rest) {
        if (ToHStringUtils.hasText(suffix)) {
            metadataCommands.set(sender, name, MetadataConstants.SUFFIX_KEY, suffix, rest);
        }
        else {
            metadataCommands.unset(sender, name, MetadataConstants.SUFFIX_KEY);
        }
    }

    private void calculateChildPermissions(Map<String, Boolean> permissions, Map<String, Boolean> children, boolean invert) {
        for (Map.Entry<String, Boolean> me : children.entrySet()) {
            String key = me.getKey().toLowerCase();
            Permission perm = Bukkit.getPluginManager().getPermission(key);
            boolean value = me.getValue() ^ invert;
            
            permissions.put(key, value);
            
            if (perm != null) {
                calculateChildPermissions(permissions, perm.getChildren(), !value);
            }
        }
    }

    protected String formatEntry(CommandSender sender, Entry e) {
        return String.format(colorize("{DARK_GREEN}- {GOLD}%s%s%s{DARK_GREEN}: {GREEN}%s"),
                (e.getRegion() == null ? "" : e.getRegion().getName() + colorize("{DARK_GREEN}/{GOLD}")),
                (e.getWorld() == null ? "" : e.getWorld().getName() + colorize("{DARK_GREEN}:{GOLD}")),
                e.getPermission(),
                e.isValue());
    }

    protected void handleMissingGroup(CommandSender sender, MissingGroupException e) {
        sendMessage(sender, colorize("{RED}Group {DARK_GREEN}%s{RED} does not exist."), e.getGroupName());
        abortBatchProcessing();
    }

}
