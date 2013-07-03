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

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.log;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.broadcastAdmin;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;
import static org.tyrannyofheaven.bukkit.util.command.reader.CommandReader.abortBatchProcessing;
import static org.tyrannyofheaven.bukkit.util.permissions.PermissionUtils.requirePermission;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
import org.tyrannyofheaven.bukkit.zPermissions.PermissionsResolver;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsConfig;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsCore;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.storage.StorageStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.util.ModelDumper;
import org.tyrannyofheaven.bukkit.zPermissions.util.Utils;

/**
 * Handler for sub-commands of /permissions
 * 
 * @author zerothangel
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

    // The "/permissions player" handler
    private final PlayerCommands playerCommand;

    // The "/permissions group" handler
    private final GroupCommands groupCommand;

    private PurgeCode purgeCode;

    private final Random random = new Random();

    SubCommands(ZPermissionsCore core, StorageStrategy storageStrategy, PermissionsResolver resolver, ModelDumper modelDumper, ZPermissionsConfig config, Plugin plugin) {
        this.core = core;
        this.storageStrategy = storageStrategy;
        this.resolver = resolver;
        this.modelDumper = modelDumper;
        this.config = config;
        this.plugin = plugin;

        playerCommand = new PlayerCommands(core, storageStrategy, resolver, config, plugin);
        groupCommand = new GroupCommands(core, storageStrategy, resolver, config, plugin);
    }

    @Command(value={"player", "pl", "p"}, description="Player-related commands")
    @Require("zpermissions.player")
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
                .forCommand("show")
                .forCommand("dump")
                .forCommand("diff")
                .forCommand("clone")
                .forCommand("rename")
                .forCommand("has")
                .forCommand("metadata")
                .forCommand("prefix")
                .forCommand("suffix")
                .forCommand("settrack")
                .show();
            return null;
        }
        
        // Stuff name into session for next handler
        session.setValue("entityName", playerName);
        return playerCommand;
    }

    @Command(value={"group", "gr", "g"}, description="Group-related commands")
    @Require("zpermissions.group")
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
            return null;
        }

        // Stuff name into session for next handler
        session.setValue("entityName", groupName);
        return groupCommand;
    }

    @Command(value={"list", "ls"}, description="List players or groups in the database")
    @Require("zpermissions.list")
    public void list(CommandSender sender, @Option(value="what", completer="constant:groups players") String what) {
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

        List<String> entityNames = storageStrategy.getDao().getEntityNames(group);
        Collections.sort(entityNames, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return a.compareToIgnoreCase(b);
            }
        });

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
        List<Utils.PermissionInfo> permissions = new ArrayList<Utils.PermissionInfo>();
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
    public void refresh(CommandSender sender) {
        core.refresh(new Runnable() {
            @Override
            public void run() {
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
            })) {
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
    public void mygroups(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, colorize("{RED}Command only valid for players."));
            return;
        }
        
        List<Membership> memberships = storageStrategy.getDao().getGroups(sender.getName());
        Collections.reverse(memberships); // Order from highest to lowest

        String groups = Utils.displayGroups(resolver.getDefaultGroup(), memberships);
        
        sendMessage(sender, colorize("{YELLOW}You are a member of: %s"), groups);
    }

    @Command(value="diff", description="Compare effective permissions of a player")
    public void diff(CommandSender sender, @Option(value={"-r", "--region", "--regions"}, valueName="regions") String regions, @Option(value={"-R", "--other-region", "--other-region"}, valueName="other-regions") String otherRegions,
            @Option(value={"-f", "--filter"}, valueName="filter") String filter, @Option(value="qualified-player", completer="player") String player, @Option(value="other-qualified-player", completer="player", optional=true) String otherPlayer) {
        List<String> header = new ArrayList<String>();

        // Parse qualifiers for first player
        final QualifiedPlayer qplayer = new QualifiedPlayer(player);
        Player p = Bukkit.getPlayerExact(qplayer.getPlayerName());
        final String worldName;
        final Set<String> regionNames;
        if (otherPlayer != null) {
            Utils.validatePlayer(storageStrategy.getDao(), resolver.getDefaultGroup(), qplayer.getPlayerName(), header);
            worldName = determineWorldName(sender, qplayer, header);
            if (worldName == null) return;
            regionNames = parseRegions(regions);
        }
        else {
            // Player must be online
            if (p == null) {
                sendMessage(sender, colorize("{RED}Player is not online."));
                return;
            }
            if (qplayer.getWorld() != null)
                header.add(colorize("{GRAY}(World qualifier ignored when comparing against Bukkit effective permissions)"));
            worldName = p.getWorld().getName().toLowerCase();
            if (!parseRegions(regions).isEmpty())
                header.add(colorize("{GRAY}(Specified regions ignored when comparing against Bukkit effective permissions)"));
            regionNames = core.getRegions(p.getLocation());
        }

        // Parse qualifiers for second player
        final QualifiedPlayer qother;
        final String otherWorldName;
        final Set<String> otherRegionNames;
        if (otherPlayer != null) {
            qother = new QualifiedPlayer(otherPlayer);
            Utils.validatePlayer(storageStrategy.getDao(), resolver.getDefaultGroup(), qother.getPlayerName(), header);
            otherWorldName = determineWorldName(sender, qother, header);
            if (otherWorldName == null) return;
            otherRegionNames = parseRegions(otherRegions);
        }
        else {
            qother = null;
            otherWorldName = null;
            otherRegionNames = Collections.emptySet();
        }

        if (qother != null) {
            // Diff one against the other
            Map<String, Boolean> rootPermissions = storageStrategy.getTransactionStrategy().execute(new TransactionCallback<Map<String, Boolean>>() {
                @Override
                public Map<String, Boolean> doInTransaction() throws Exception {
                    return resolver.resolvePlayer(qplayer.getPlayerName().toLowerCase(), worldName, regionNames).getPermissions();
                }
            });
            Map<String, Boolean> permissions = new HashMap<String, Boolean>();
            Utils.calculateChildPermissions(permissions, rootPermissions, false);
            
            Map<String, Boolean> otherRootPermissions = storageStrategy.getTransactionStrategy().execute(new TransactionCallback<Map<String, Boolean>>() {
                @Override
                public Map<String, Boolean> doInTransaction() throws Exception {
                    return resolver.resolvePlayer(qother.getPlayerName().toLowerCase(), otherWorldName, otherRegionNames).getPermissions();
                }
            });
            Map<String, Boolean> otherPermissions = new HashMap<String, Boolean>();
            Utils.calculateChildPermissions(otherPermissions, otherRootPermissions, false);

            Utils.displayPermissionsDiff(plugin, sender, permissions, otherPermissions, header,
                    String.format(colorize("{AQUA}%s {YELLOW}on %s%s {WHITE}adds{YELLOW}:"), qother.getPlayerName(), otherWorldName,
                            (otherRegionNames.isEmpty() ? "" : "[" + ToHStringUtils.delimitedString(",", otherRegionNames) + "]")),
                    String.format(colorize("{AQUA}%s {YELLOW}on %s%s {WHITE}removes{YELLOW}:"), qother.getPlayerName(), otherWorldName,
                            (otherRegionNames.isEmpty() ? "" : "[" + ToHStringUtils.delimitedString(",", otherRegionNames) + "]")),
                    String.format(colorize("{AQUA}%s {YELLOW}on %s%s {WHITE}changes{YELLOW}:"), qother.getPlayerName(), otherWorldName,
                            (otherRegionNames.isEmpty() ? "" : "[" + ToHStringUtils.delimitedString(",", otherRegionNames) + "]")),
                    String.format(colorize("{YELLOW}Players on %s%s have identical effective permissions."), otherWorldName,
                            (otherRegionNames.isEmpty() ? "" : "[" + ToHStringUtils.delimitedString(",", otherRegionNames) + "]")), filter);
        }
        else {
            // Diff Bukkit effective permissions against zPerms effective permissions
            Map<String, Boolean> rootPermissions = storageStrategy.getTransactionStrategy().execute(new TransactionCallback<Map<String, Boolean>>() {
                @Override
                public Map<String, Boolean> doInTransaction() throws Exception {
                    return resolver.resolvePlayer(qplayer.getPlayerName().toLowerCase(), worldName, regionNames).getPermissions();
                }
            });
            Map<String, Boolean> permissions = new HashMap<String, Boolean>();
            Utils.calculateChildPermissions(permissions, rootPermissions, false);

            // Get Bukkit effective permissions
            Map<String, Boolean> otherPermissions = new HashMap<String, Boolean>();
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

    private String determineWorldName(CommandSender sender, QualifiedPlayer qplayer, List<String> header) {
        if (qplayer.getWorld() == null) {
            String worldName;
            if (sender instanceof Player) {
                // Use sender's world
                worldName = ((Player)sender).getWorld().getName();
            }
            else {
                // Default to first world
                worldName = Bukkit.getWorlds().get(0).getName();
            }
            header.add(String.format(colorize("{GRAY}(Assuming world \"%s\" for player \"%s\")"), worldName, qplayer.getPlayerName()));
            return worldName.toLowerCase();
        }
        else {
            // Just verify that the given world exists
            if (Bukkit.getWorld(qplayer.getWorld()) == null) {
                sendMessage(sender, colorize("{RED}Invalid world for player \"%s\"."), qplayer.getPlayerName());
                return null;
            }
            return qplayer.getWorld().toLowerCase();
        }
    }

    private Set<String> parseRegions(String regions) {
        if (regions == null)
            return Collections.emptySet();
        Set<String> result = new LinkedHashSet<String>();
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
                            storageStrategy.getDao().deleteEntity(player.getDisplayName(), false);
                        }
                        // Purge groups
                        for (PermissionEntity group : storageStrategy.getDao().getEntities(true)) {
                            storageStrategy.getDao().deleteEntity(group.getDisplayName(), true);
                        }
                    }
                });
                broadcastAdmin(plugin, "%s performed full permissions purge", sender.getName());
                sendMessage(sender, colorize("{YELLOW}Full permissions purge successful."));
                purgeCode = null;
                
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

    private static class QualifiedPlayer {
        
        private final String world;
        
        private final String playerName;
        
        private QualifiedPlayer(String qualifiedPlayer) {
            String[] parts = qualifiedPlayer.split(":", 2);
            if (parts.length > 1) {
                world = parts[0];
                playerName = parts[1];
            }
            else {
                world = null;
                playerName = parts[0];
            }
        }

        public String getWorld() {
            return world;
        }

        public String getPlayerName() {
            return playerName;
        }

    }

}
