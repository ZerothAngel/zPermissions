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

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.log;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.broadcastAdmin;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;
import static org.tyrannyofheaven.bukkit.util.command.reader.CommandReader.abortBatchProcessing;
import static org.tyrannyofheaven.bukkit.util.permissions.PermissionUtils.requirePermission;
import static org.tyrannyofheaven.bukkit.zPermissions.util.Utils.formatPlayerName;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.util.ToHStringUtils;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.CommandSession;
import org.tyrannyofheaven.bukkit.util.command.HelpBuilder;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.ParseException;
import org.tyrannyofheaven.bukkit.util.command.Require;
import org.tyrannyofheaven.bukkit.util.command.reader.CommandReader;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.util.uuid.CommandUuidResolver;
import org.tyrannyofheaven.bukkit.util.uuid.CommandUuidResolverHandler;
import org.tyrannyofheaven.bukkit.zPermissions.PermissionsResolver;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsConfig;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsCore;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.storage.StorageStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.util.MetadataConstants;
import org.tyrannyofheaven.bukkit.zPermissions.util.ModelDumper;
import org.tyrannyofheaven.bukkit.zPermissions.util.SearchTask;
import org.tyrannyofheaven.bukkit.zPermissions.util.Utils;

/**
 * Handler for sub-commands of /permissions
 * 
 * @author asaddi
 */
public class SubCommands {

    private static final long PURGE_CODE_EXPIRATION = 30000L; // 30 secs

    private final ZPermissionsCore core;

    private final StorageStrategy storageStrategy;

    private final PermissionsResolver resolver;

    private final ModelDumper modelDumper;

    private final ZPermissionsConfig config;

    // Parent plugin
    private final Plugin plugin;

    private final CommandUuidResolver uuidResolver;

    // The "/permissions player" handler
    private final PlayerCommands playerCommand;

    // The "/permissions group" handler
    private final GroupCommands groupCommand;

    private PurgeCode purgeCode;

    private final Random random = new Random();

    SubCommands(ZPermissionsCore core, StorageStrategy storageStrategy, PermissionsResolver resolver, ModelDumper modelDumper, ZPermissionsConfig config, Plugin plugin, CommandUuidResolver uuidResolver) {
        this.core = core;
        this.storageStrategy = storageStrategy;
        this.resolver = resolver;
        this.modelDumper = modelDumper;
        this.config = config;
        this.plugin = plugin;
        this.uuidResolver = uuidResolver;

        playerCommand = new PlayerCommands(core, storageStrategy, resolver, config, plugin, uuidResolver);
        groupCommand = new GroupCommands(core, storageStrategy, resolver, config, plugin, uuidResolver);
    }

    @Command(value={"player", "pl", "p"}, description="Player-related commands")
    @Require({"zpermissions.player.view", "zpermissions.player.manage", "zpermissions.player.chat"})
    public PlayerCommands player(HelpBuilder helpBuilder, CommandSender sender, CommandSession session, @Option(value="player", nullable=true, completer="player") String playerName, String[] args) {
        if (args.length == 0) {
            // Display sub-command help
            helpBuilder.withCommandSender(sender)
                .withHandler(playerCommand)
                .forCommand("get")
                .forCommand("set")
                .forCommand("unset")
                .forCommand("settemp")
                .forCommand("purge")
                .forCommand("groups")
                .forCommand("setgroup")
                .forCommand("addgroup")
                .forCommand("removegroup")
                .forCommand("show")
                .forCommand("dump")
                .forCommand("diff")
                .forCommand("clone")
                .forCommand("has")
                .forCommand("metadata")
                .forCommand("prefix")
                .forCommand("suffix")
                .forCommand("settrack")
                .show();
            abortBatchProcessing();
            return null;
        }
        
        // Stuff name into session for next handler
        session.setValue("entityName", playerName);
        return playerCommand;
    }

    @Command(value={"group", "gr", "g"}, description="Group-related commands")
    @Require({"zpermissions.group.view", "zpermissions.group.manage", "zpermissions.group.chat"})
    public CommonCommands group(HelpBuilder helpBuilder, CommandSender sender, CommandSession session, @Option(value="group", nullable=true, completer="group") String groupName, String[] args) {
        if (args.length == 0) {
            // Display sub-command help
            helpBuilder.withCommandSender(sender)
                .withHandler(groupCommand)
                .forCommand("create")
                .forCommand("get")
                .forCommand("set")
                .forCommand("unset")
                .forCommand("purge")
                .forCommand("members")
                .forCommand("setparents")
                .forCommand("setweight")
                .forCommand("add")
                .forCommand("remove")
                .forCommand("show")
                .forCommand("dump")
                .forCommand("diff")
                .forCommand("clone")
                .forCommand("rename")
                .forCommand("metadata")
                .forCommand("prefix")
                .forCommand("suffix")
                .show();
            abortBatchProcessing();
            return null;
        }

        // Stuff name into session for next handler
        session.setValue("entityName", groupName);
        return groupCommand;
    }

    @Command(value={"list", "ls"}, description="List players or groups in the database")
    @Require("zpermissions.list")
    public void list(CommandSender sender, @Option(value={"-U", "--uuid"}) boolean showUuid, @Option(value="what", completer="constant:groups players") String what) {
        boolean group;
        if ("groups".startsWith(what)) {
            group = true;
        }
        else if ("players".startsWith(what)) {
            group = false;
        }
        else {
            throw new ParseException("<what> should be 'groups' or 'players'");
        }

        List<PermissionEntity> entities = storageStrategy.getDao().getEntities(group);
        Collections.sort(entities, new Comparator<PermissionEntity>() {
            @Override
            public int compare(PermissionEntity a, PermissionEntity b) {
                return a.getDisplayName().toLowerCase().compareTo(b.getDisplayName().toLowerCase());
            }
        });
        List<String> entityNames = new ArrayList<>(entities.size());
        for (PermissionEntity entity : entities) {
            entityNames.add(group ? entity.getDisplayName() : formatPlayerName(entity, showUuid));
        }

        if (entityNames.isEmpty()) {
            sendMessage(sender, colorize("{YELLOW}No %s found."), group ? "groups" : "players");
        }
        else {
            for (String entityName : entityNames) {
                sendMessage(sender, colorize("{DARK_GREEN}- %s"), entityName);
            }
        }
    }

    @Command(value="check", description="Check against effective permissions")
    @Require("zpermissions.check")
    public void check(CommandSender sender, @Option("permission") String permission, @Option(value="player", optional=true, completer="player") String playerName) {
        Player player;
        if (playerName == null) {
            // No player specified
            if (!(sender instanceof Player)) {
                sendMessage(sender, colorize("{RED}Cannot check permissions of console."));
                abortBatchProcessing();
                return;
            }
            // Use sender
            player = (Player)sender;
        }
        else {
            // Checking perms for another player
            requirePermission(sender, "zpermissions.check.other");

            player = Bukkit.getPlayer(playerName);
            if (player == null) {
                sendMessage(sender, colorize("{RED}Player is not online."));
                abortBatchProcessing();
                return;
            }
        }

        // Scan effective permissions
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            if (permission.equalsIgnoreCase(pai.getPermission())) {
                sendMessage(sender, colorize("{AQUA}%s{YELLOW} sets {GOLD}%s{YELLOW} to {GREEN}%s"), player.getName(), pai.getPermission(), pai.getValue());
                return;
            }
        }
        sendMessage(sender, colorize("{AQUA}%s{YELLOW} does not set {GOLD}%s"), player.getName(), permission);
    }

    @Command(value="inspect", description="Inspect effective permissions")
    @Require("zpermissions.inspect")
    public void inspect(CommandSender sender, @Option(value={"-f", "--filter"}, valueName="filter") String filter, @Option({"-v", "--verbose"}) boolean verbose, @Option(value="player", optional=true, completer="player") String playerName) {
        Player player;
        if (playerName == null) {
            // No player specified
            if (!(sender instanceof Player)) {
                sendMessage(sender, colorize("{RED}Cannot inspect permissions of console."));
                abortBatchProcessing();
                return;
            }
            // Use sender
            player = (Player)sender;
        }
        else {
            // Checking perms for another player
            requirePermission(sender, "zpermissions.inspect.other");

            player = Bukkit.getPlayer(playerName);
            if (player == null) {
                sendMessage(sender, colorize("{RED}Player is not online."));
                abortBatchProcessing();
                return;
            }
        }

        // Build map of effective permissions
        List<Utils.PermissionInfo> permissions = new ArrayList<>();
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            permissions.add(new Utils.PermissionInfo(pai.getPermission(), pai.getValue(), pai.getAttachment() != null ? pai.getAttachment().getPlugin().getName() : "default"));
        }
        
        Utils.displayPermissions(plugin, sender, null, permissions, filter, sender instanceof ConsoleCommandSender || verbose);
    }

    @Command(value="reload", description="Re-read config.yml")
    @Require("zpermissions.reload")
    public void reload(CommandSender sender) {
        core.reload();
        sendMessage(sender, colorize("{WHITE}config.yml{YELLOW} reloaded"));
    }

    @Command(value="refresh", description="Re-read permissions from storage")
    @Require("zpermissions.refresh")
    public void refresh(CommandSender sender, @Option(value={ "-c", "--conditional" }, optional=true) Boolean conditional) {
        if (conditional == null)
            conditional = Boolean.FALSE; // backwards compatibility
        core.refresh(!conditional, new Runnable() {
            @Override
            public void run() {
                core.invalidateMetadataCache();
                core.refreshPlayers();
                core.refreshExpirations();
            }
        });
        sendMessage(sender, colorize("{YELLOW}Refresh queued."));
    }

    // Ensure filename doesn't have any funny characters
    private File sanitizeFilename(File dir, String filename) {
        String[] parts = filename.split(File.separatorChar == '\\' ? "\\\\" : File.separator);
        if (parts.length == 1) {
            if (!parts[0].startsWith("."))
                return new File(dir, filename);
        }
        throw new ParseException("Invalid filename.");
    }

    @Command(value={"import", "restore"}, description="Import a dump of the database")
    @Require("zpermissions.import")
    public void import_command(final CommandSender sender, @Option(value="filename", completer="dump-dir") String filename) {
        File inFile = sanitizeFilename(config.getDumpDirectory(), filename);
        try {
            // Ensure database is empty
            if (!storageStrategy.getTransactionStrategy().execute(new TransactionCallback<Boolean>() {
                @Override
                public Boolean doInTransaction() throws Exception {
                    // Check in a single transaction
                    List<PermissionEntity> players = storageStrategy.getDao().getEntities(false);
                    List<PermissionEntity> groups = storageStrategy.getDao().getEntities(true);
                    if (!players.isEmpty() || !groups.isEmpty()) {
                        sendMessage(sender, colorize("{RED}Database is not empty!"));
                        return false;
                    }
                    return true;
                }
            }, true)) {
                return;
            }

            // Execute commands
            if (CommandReader.read(Bukkit.getServer(), sender, inFile, plugin)) {
                sendMessage(sender, colorize("{YELLOW}Import complete."));
            }
            else {
                sendMessage(sender, colorize("{RED}Import failed."));
            }
        }
        catch (IOException e) {
            sendMessage(sender, colorize("{RED}Error importing; see server log."));
            log(plugin, Level.SEVERE, "Error importing:", e);
        }
    }
    
    @Command(value={"export", "dump"}, description="Export a dump of the database")
    @Require("zpermissions.export")
    public void export(CommandSender sender, @Option(value="filename", completer="dump-dir") String filename) {
        File outFile = sanitizeFilename(config.getDumpDirectory(), filename);
        try {
            if (!config.getDumpDirectory().exists()) {
                if (!config.getDumpDirectory().mkdirs()) {
                    sendMessage(sender, colorize("{RED}Unable to create dump directory"));
                    return;
                }
            }
            modelDumper.dump(outFile);
            sendMessage(sender, colorize("{YELLOW}Export completed."));
        }
        catch (IOException e) {
            sendMessage(sender, colorize("{RED}Error exporting; see server log."));
            log(plugin, Level.SEVERE, "Error exporting:", e);
        }
    }

    @Command(value="mygroups", description="List groups you are a member of")
    @Require("zpermissions.mygroups")
    public void mygroups(CommandSender sender, @Option({"-v", "--verbose"}) boolean verbose) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, colorize("{RED}Command only valid for players."));
            return;
        }
        
        List<Membership> memberships = storageStrategy.getDao().getGroups(((Player)sender).getUniqueId());
        if (!verbose)
            memberships = Utils.filterExpired(memberships);
        Collections.reverse(memberships); // Order from highest to lowest

        String groups = Utils.displayGroups(resolver.getDefaultGroup(), memberships);
        
        sendMessage(sender, colorize("{YELLOW}You are a member of: %s"), groups);
    }

    @Command(value="prefix", description="Modify your chat prefix")
    @Require("zpermissions.mychat")
    public void prefix(final CommandSender sender, @Option({"-c", "--clear"}) boolean clear, @Option(value="prefix", optional=true) String prefix, String[] rest) {
        selfServeChat(sender, MetadataConstants.PREFIX_KEY, clear, prefix, rest);
    }

    @Command(value="suffix", description="Modify your chat suffix")
    @Require("zpermissions.mychat")
    public void suffix(final CommandSender sender, @Option({"-c", "--clear"}) boolean clear, @Option(value="suffix", optional=true) String suffix, String[] rest) {
        selfServeChat(sender, MetadataConstants.SUFFIX_KEY, clear, suffix, rest);
    }

    private void selfServeChat(CommandSender sender, String metadataName, boolean clear, String value, String[] rest) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, colorize("{RED}Command only valid for players."));
            return;
        }
        if ((value != null && !value.isEmpty()) || rest.length > 0) {
            setPlayerMetadataString((Player)sender, metadataName, value, rest);
        }
        else if (clear) {
            unsetPlayerMetadataString((Player)sender, metadataName);
        }
        else {
            showPlayerMetadataString((Player)sender, metadataName);
        }
    }

    // Possible dupe with stuff in MetadataCommands. Refactor someday?
    private void showPlayerMetadataString(final Player sender, final String metadataName) {
        Object result = storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallback<Object>() {
            @Override
            public Object doInTransaction() throws Exception {
                return storageStrategy.getDao().getMetadata(sender.getName(), sender.getUniqueId(), false, metadataName);
            }
        }, true);

        if (result == null) {
            sendMessage(sender, colorize("{YELLOW}You do not have a {GOLD}%s"), metadataName);
            abortBatchProcessing();
        }
        else {
            sendMessage(sender, colorize("{YELLOW}Your {GOLD}%s{YELLOW} is {GREEN}%s"), metadataName, result);
            sendMessage(sender, colorize("{GRAY}(Add -c option to clear)"));
        }
    }

    // Possible dupe with stuff in MetadataCommands. Refactor someday?
    private void setPlayerMetadataString(final Player player, final String metadataName, String value, String[] rest) {
        final StringBuilder stringValue = new StringBuilder(value);
        if (rest.length > 0) {
            stringValue.append(' ')
                .append(ToHStringUtils.delimitedString(" ", (Object[])rest));
        }
        storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                storageStrategy.getDao().setMetadata(player.getName(), player.getUniqueId(), false, metadataName, stringValue.toString());
            }
        });

        sendMessage(player, colorize("{YELLOW}Your {GOLD}%s{YELLOW} has been set to {GREEN}%s{YELLOW}"), metadataName, stringValue);
        core.invalidateMetadataCache(player.getName(), player.getUniqueId(), false);
    }

    // Possible dupe with stuff in MetadataCommands. Refactor someday?
    private void unsetPlayerMetadataString(final Player player, final String metadataName) {
        Boolean result = storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                return storageStrategy.getDao().unsetMetadata(player.getName(), player.getUniqueId(), false, metadataName);
            }
        });
        
        if (result) {
            sendMessage(player, colorize("{YELLOW}Your {GOLD}%s{YELLOW} has been unset"), metadataName);
            core.invalidateMetadataCache(player.getName(), player.getUniqueId(), false);
        }
        else {
            sendMessage(player, colorize("{YELLOW}You do not have a {GOLD}%s"), metadataName);
            abortBatchProcessing();
        }
    }

    @Command(value="diff", description="Compare effective permissions of a player")
    public void diff(CommandSender sender, final @Option(value={"-r", "--region", "--regions"}, valueName="regions") String regions, final @Option(value={"-R", "--other-region", "--other-region"}, valueName="other-regions") String otherRegions,
            final @Option(value={"-w", "--world"}, valueName="world") String world, final @Option(value={"-W", "--other-world"}, valueName="other-world") String otherWorld,
            final @Option(value={"-f", "--filter"}, valueName="filter") String filter, final @Option(value="player", completer="player") String player, final @Option(value="other-player", completer="player", optional=true) String otherPlayer) {
        final SubCommands realThis = this;
        uuidResolver.resolveUsername(sender, player, false, new CommandUuidResolverHandler() {
            @Override
            public void process(CommandSender sender, final String name, final UUID uuid, boolean group) {
                // Resolve other name (if present)
                uuidResolver.resolveUsername(sender, otherPlayer, false, new CommandUuidResolverHandler() {
                    @Override
                    public void process(CommandSender sender, String otherPlayer, UUID otherUuid, boolean group) {
                        realThis.diff(sender, regions, otherRegions, world, otherWorld, filter, name, uuid, otherPlayer, otherUuid);
                    }
                });
            }
        });
    }

    private void diff(CommandSender sender, String regions, String otherRegions,
            String world, String otherWorld,
            String filter, String player, final UUID uuid, String otherPlayer, final UUID otherUuid) {
        List<String> header = new ArrayList<>();

        // Parse qualifiers for first player
        Player p = Bukkit.getPlayer(uuid);
        final String worldName;
        final Set<String> regionNames;
        if (otherPlayer != null) {
            Utils.validatePlayer(storageStrategy.getDao(), resolver.getDefaultGroup(), uuid, player, header);
            worldName = determineWorldName(sender, world, player, header);
            if (worldName == null) return;
            regionNames = parseRegions(regions);
        }
        else {
            // Player must be online
            if (p == null) {
                sendMessage(sender, colorize("{RED}Player is not online."));
                return;
            }
            if (world != null)
                header.add(colorize("{GRAY}(World qualifier ignored when comparing against Bukkit effective permissions)"));
            worldName = p.getWorld().getName().toLowerCase();
            if (!parseRegions(regions).isEmpty())
                header.add(colorize("{GRAY}(Specified regions ignored when comparing against Bukkit effective permissions)"));
            regionNames = core.getRegions(p.getLocation(), p);
        }

        // Parse qualifiers for second player
        final String otherWorldName;
        final Set<String> otherRegionNames;
        if (otherPlayer != null) {
            Utils.validatePlayer(storageStrategy.getDao(), resolver.getDefaultGroup(), otherUuid, otherPlayer, header);
            otherWorldName = determineWorldName(sender, otherWorld, otherPlayer, header);
            if (otherWorldName == null) return;
            otherRegionNames = parseRegions(otherRegions);
        }
        else {
            otherWorldName = null;
            otherRegionNames = Collections.emptySet();
        }

        if (otherPlayer != null) {
            // Diff one against the other
            Map<String, Boolean> rootPermissions = storageStrategy.getTransactionStrategy().execute(new TransactionCallback<Map<String, Boolean>>() {
                @Override
                public Map<String, Boolean> doInTransaction() throws Exception {
                    return resolver.resolvePlayer(uuid, worldName, regionNames).getPermissions();
                }
            }, true);
            Map<String, Boolean> permissions = new HashMap<>();
            Utils.calculateChildPermissions(permissions, rootPermissions, false);
            
            Map<String, Boolean> otherRootPermissions = storageStrategy.getTransactionStrategy().execute(new TransactionCallback<Map<String, Boolean>>() {
                @Override
                public Map<String, Boolean> doInTransaction() throws Exception {
                    return resolver.resolvePlayer(otherUuid, otherWorldName, otherRegionNames).getPermissions();
                }
            }, true);
            Map<String, Boolean> otherPermissions = new HashMap<>();
            Utils.calculateChildPermissions(otherPermissions, otherRootPermissions, false);

            Utils.displayPermissionsDiff(plugin, sender, permissions, otherPermissions, header,
                    String.format(colorize("{AQUA}%s {YELLOW}on %s%s {WHITE}adds{YELLOW}:"), otherPlayer, otherWorldName,
                            (otherRegionNames.isEmpty() ? "" : "[" + ToHStringUtils.delimitedString(",", otherRegionNames) + "]")),
                    String.format(colorize("{AQUA}%s {YELLOW}on %s%s {WHITE}removes{YELLOW}:"), otherPlayer, otherWorldName,
                            (otherRegionNames.isEmpty() ? "" : "[" + ToHStringUtils.delimitedString(",", otherRegionNames) + "]")),
                    String.format(colorize("{AQUA}%s {YELLOW}on %s%s {WHITE}changes{YELLOW}:"), otherPlayer, otherWorldName,
                            (otherRegionNames.isEmpty() ? "" : "[" + ToHStringUtils.delimitedString(",", otherRegionNames) + "]")),
                    String.format(colorize("{YELLOW}Players on %s%s have identical effective permissions."), otherWorldName,
                            (otherRegionNames.isEmpty() ? "" : "[" + ToHStringUtils.delimitedString(",", otherRegionNames) + "]")), filter);
        }
        else {
            // Diff Bukkit effective permissions against zPerms effective permissions
            Map<String, Boolean> rootPermissions = storageStrategy.getTransactionStrategy().execute(new TransactionCallback<Map<String, Boolean>>() {
                @Override
                public Map<String, Boolean> doInTransaction() throws Exception {
                    return resolver.resolvePlayer(uuid, worldName, regionNames).getPermissions();
                }
            }, true);
            Map<String, Boolean> permissions = new HashMap<>();
            Utils.calculateChildPermissions(permissions, rootPermissions, false);

            // Get Bukkit effective permissions
            Map<String, Boolean> otherPermissions = new HashMap<>();
            for (PermissionAttachmentInfo pai : p.getEffectivePermissions()) {
                otherPermissions.put(pai.getPermission().toLowerCase(), pai.getValue());
            }

            Utils.displayPermissionsDiff(plugin, sender, permissions, otherPermissions, header,
                    colorize("{YELLOW}Bukkit effective permissions {WHITE}add:"),
                    colorize("{YELLOW}Bukkit effective permissions {WHITE}remove:"),
                    colorize("{YELLOW}Bukkit effective permissions {WHITE}change:"),
                    colorize("{YELLOW}Bukkit effective permissions are identical."), filter);
        }
    }

    private String determineWorldName(CommandSender sender, String world, String player, List<String> header) {
        if (world == null) {
            String worldName;
            if (sender instanceof Player) {
                // Use sender's world
                worldName = ((Player)sender).getWorld().getName();
            }
            else {
                // Default to first world
                worldName = Bukkit.getWorlds().get(0).getName();
            }
            header.add(String.format(colorize("{GRAY}(Assuming world \"%s\" for player \"%s\")"), worldName, player));
            return worldName.toLowerCase();
        }
        else {
            // Just verify that the given world exists
            if (Bukkit.getWorld(world) == null) {
                sendMessage(sender, colorize("{RED}Invalid world for player \"%s\"."), player);
                return null;
            }
            return world.toLowerCase();
        }
    }

    private Set<String> parseRegions(String regions) {
        if (regions == null)
            return Collections.emptySet();
        Set<String> result = new LinkedHashSet<>();
        for (String region : regions.split("\\s*,\\s*")) {
            result.add(region.toLowerCase());
        }
        return result;
    }

    @Command(value="purge", description="Delete all players and groups")
    @Require("zpermissions.purge")
    public void purge(CommandSender sender, @Option(value="code", optional=true) Integer code) {
        if (purgeCode != null) {
            if (purgeCode.getTimestamp() < System.currentTimeMillis() - PURGE_CODE_EXPIRATION) {
                // Past expiration
                sendMessage(sender, colorize("{RED}Too slow. Try again without code."));
                purgeCode = null;
            }
            else if (!purgeCode.getExecutor().equals(sender.getName())) {
                sendMessage(sender, colorize("{RED}Confirmation pending for %s. Ignored."), purgeCode.getExecutor());
            }
            else if (code == null) {
                sendMessage(sender, colorize("{RED}Confirmation pending. Try again with code."));
            }
            else if (purgeCode.getCode() != code) {
                sendMessage(sender, colorize("{RED}Code mismatch. Try again."));
            }
            else {
                storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
                    @Override
                    public void doInTransactionWithoutResult() throws Exception {
                        // Purge players
                        for (PermissionEntity player : storageStrategy.getDao().getEntities(false)) {
                            storageStrategy.getDao().deleteEntity(player.getDisplayName(), player.getUuid(), false);
                        }
                        // Purge groups
                        for (PermissionEntity group : storageStrategy.getDao().getEntities(true)) {
                            storageStrategy.getDao().deleteEntity(group.getDisplayName(), null, true);
                        }
                    }
                });
                broadcastAdmin(plugin, "%s performed full permissions purge", sender.getName());
                sendMessage(sender, colorize("{YELLOW}Full permissions purge successful."));
                purgeCode = null;
                
                core.invalidateMetadataCache();
                core.refreshPlayers();
                core.refreshExpirations();
            }
        }
        else {
            if (code != null) {
                sendMessage(sender, colorize("{RED}No purge pending. Try again without code."));
            }
            else {
                int codeNumber = 100000 + random.nextInt(900000); // random 6 digit number
                purgeCode = new PurgeCode(sender.getName(), codeNumber, System.currentTimeMillis());
                sendMessage(sender, colorize("{YELLOW}Issue {DARK_GRAY}/permissions purge %d{YELLOW} to confirm."), codeNumber);
            }
        }
    }

    @Command(value={"cleanup", "gc"}, description="Perform optional cleanup of permissions storage")
    @Require("zpermissions.cleanup")
    public void cleanup(CommandSender sender) {
        storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                List<Membership> toDelete = new ArrayList<>();
                Date now = new Date();
                // For each group...
                for (PermissionEntity group : storageStrategy.getDao().getEntities(true)) {
                    // Check each membership
                    for (Membership membership : group.getMemberships()) {
                        if (membership.getExpiration() != null && !membership.getExpiration().after(now)) {
                            // Expired
                            toDelete.add(membership);
                        }
                    }
                }
                
                // This is going to be slow and inefficient since the DAO scans each member
                for (Membership membership : toDelete) {
                    storageStrategy.getDao().removeMember(membership.getGroup().getDisplayName(), membership.getUuid());
                }
            }
        });
        broadcastAdmin(plugin, "%s performed cleanup", sender.getName());
        sendMessage(sender, colorize("{YELLOW}Cleanup successful."));

        // Theoretically we haven't touched any visible memberships. However,
        // just in case something expired during cleanup...
        core.invalidateMetadataCache();
        core.refreshPlayers();
        core.refreshExpirations();
    }

    @Command(value="search", description="Search for players or groups that have a specific permission")
    @Require("zpermissions.search")
    public void search(CommandSender sender, @Option(value={"-U", "--uuid"}) boolean showUuid, @Option(value={"-p", "--players"}) boolean searchPlayers, @Option(value={"-g", "--groups"}) boolean searchGroups,
            @Option(value={"-e", "--effective"}) boolean effective, @Option(value={"-w", "--world"}, valueName="world", completer="world") String worldName,
            @Option(value={"-r", "--region", "--regions"}, valueName="regions") String regions, @Option("permission") String permission) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sendMessage(sender, colorize("{GRAY}(Results only available on console.)"));
        }
        
        if (!searchPlayers && !searchGroups) {
            // Default to both
            searchPlayers = true;
            searchGroups = true;
        }
        
        Set<String> regionNames = Collections.emptySet();
        if (effective) {
            // Figure out world
            if (worldName == null) {
                if (sender instanceof Player) {
                    // Use issuer's world
                    worldName = ((Player)sender).getWorld().getName();
                    sendMessage(sender, colorize("{GRAY}(Using current world: %s. Use -w to specify a world.)"), worldName);
                }
                else {
                    // Default to first world
                    worldName = Bukkit.getWorlds().get(0).getName();
                    sendMessage(sender, colorize("{GRAY}(Use -w to specify a world. Defaulting to \"%s\")"), worldName);
                }
            }
            else {
                // Just verify it exists
                if (Bukkit.getWorld(worldName) == null) {
                    sendMessage(sender, colorize("{RED}Invalid world."));
                    return;
                }
            }
            worldName = worldName.toLowerCase();
            regionNames = parseRegions(regions);
        }
        
        if (!ToHStringUtils.hasText(permission)) {
            throw new ParseException("Permission cannot be empty");
        }

        permission = permission.trim();

        List<UUID> players = Collections.emptyList();
        if (searchPlayers) {
            players = new ArrayList<>();
            List<PermissionEntity> playerEntities = storageStrategy.getDao().getEntities(false);
            for (PermissionEntity entity : playerEntities) {
                players.add(entity.getUuid());
            }
        }
        
        List<String> groups = Collections.emptyList();
        if (searchGroups) {
            groups = storageStrategy.getDao().getEntityNames(true);
        }
        
        // Create and configure search task
        SearchTask searchTask = new SearchTask(plugin, storageStrategy, resolver, permission, players, groups, effective, worldName, regionNames, showUuid);
        searchTask.setBatchSize(config.getSearchBatchSize());
        searchTask.setDelay(config.getSearchDelay());

        sendMessage(sender, colorize("{YELLOW}Starting search (#%d) for {GOLD}%s{YELLOW}..."), searchTask.getSearchId(), permission);

        // Kick off search
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, searchTask);
    }

    private static class PurgeCode {

        private final String executor;

        private final int code;
        
        private final long timestamp;

        private PurgeCode(String executor, int code, long timestamp) {
            this.executor = executor;
            this.code = code;
            this.timestamp = timestamp;
        }

        public String getExecutor() {
            return executor;
        }

        public int getCode() {
            return code;
        }

        public long getTimestamp() {
            return timestamp;
        }

    }

}
