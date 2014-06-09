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
import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.delimitedString;
import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.hasText;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.assertFalse;
import static org.tyrannyofheaven.bukkit.util.command.reader.CommandReader.abortBatchProcessing;
import static org.tyrannyofheaven.bukkit.util.permissions.PermissionUtils.requireOnePermission;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.util.ToHLoggingUtils;
import org.tyrannyofheaven.bukkit.util.ToHMessageUtils;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.HelpBuilder;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Require;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.uuid.CommandUuidResolver;
import org.tyrannyofheaven.bukkit.util.uuid.CommandUuidResolverHandler;
import org.tyrannyofheaven.bukkit.zPermissions.PermissionsResolver;
import org.tyrannyofheaven.bukkit.zPermissions.RefreshCause;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsConfig;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsCore;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsRankChangeEvent;
import org.tyrannyofheaven.bukkit.zPermissions.dao.MissingGroupException;
import org.tyrannyofheaven.bukkit.zPermissions.storage.StorageStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.util.ModelDumper;
import org.tyrannyofheaven.bukkit.zPermissions.util.Utils;

/**
 * Handler for top-level commands:
 * <ul>
 *   <li>/permissions</li>
 *   <li>/promote</li>
 *   <li>/demote</li>
 *   <li>/setrank</li>
 *   <li>/unsetrank</li>
 * </ul>
 * 
 * @author asaddi
 */
public class RootCommands {

    private final ZPermissionsCore core;
    
    private final StorageStrategy storageStrategy;

    private final PermissionsResolver resolver;

    private final ZPermissionsConfig config;

    // Parent plugin
    private final Plugin plugin;

    private final CommandUuidResolver uuidResolver;

    // Handler for /permissions sub-commands
    private final SubCommands sc;

    private static enum BroadcastScope {
        DEFAULT(true), QUIET(false), LOUD(true), QUIET_LOUD(false);
        
        private final boolean shouldEcho;
        
        private BroadcastScope(boolean shouldEcho) {
            this.shouldEcho = shouldEcho;
        }
        
        public boolean isShouldEcho() {
            return shouldEcho;
        }

    }

    public RootCommands(ZPermissionsCore core, StorageStrategy storageStrategy, PermissionsResolver resolver, ModelDumper modelDumper, ZPermissionsConfig config, Plugin plugin, CommandUuidResolver uuidResolver) {
        this.core = core;
        this.storageStrategy = storageStrategy;
        this.resolver = resolver;
        this.config = config;
        this.plugin = plugin;
        this.uuidResolver = uuidResolver;

        sc = new SubCommands(core, storageStrategy, resolver, modelDumper, config, plugin, uuidResolver);
    }

    @Command("permissions")
    @Require({"zpermissions.player.view", "zpermissions.player.manage", "zpermissions.player.chat",
        "zpermissions.group.view", "zpermissions.group.manage", "zpermissions.group.chat",
        "zpermissions.list", "zpermissions.check", "zpermissions.reload", "zpermissions.import", "zpermissions.export", "zpermissions.inspect",
        "zpermissions.mygroups", "zpermissions.purge", "zpermissions.diff", "zpermissions.mychat"})
    public Object perm(HelpBuilder helpBuilder, CommandSender sender, String[] args) {
        if (args.length == 0) {
            helpBuilder.withCommandSender(sender)
                .withHandler(sc)
                .forCommand("player")
                .forCommand("group")
                .forCommand("list")
                .forCommand("check")
                .forCommand("inspect")
                .forCommand("diff")
                .forCommand("mygroups")
                .forCommand("prefix")
                .forCommand("suffix")
                .forCommand("reload")
                .forCommand("import")
                .forCommand("export")
                .forCommand("refresh")
                .show();
            abortBatchProcessing();
            return null;
        }
        return sc;
    }

    // Audit record of change. If quiet, only log to server log rather than
    // broadcasting to admins.
    private void announce(String notifyNode, BroadcastScope scope, String format, Object... args) {
        if (scope == BroadcastScope.DEFAULT) {
            if (config.isRankAdminBroadcast()) {
                ToHMessageUtils.broadcastAdmin(plugin, format, args);
            }
            else {
                ToHMessageUtils.broadcast(plugin, "zpermissions.notify." + notifyNode, format, args);
                ToHLoggingUtils.log(plugin, format, args); // ensure it also goes to log
            }
        }
        else if (scope == BroadcastScope.QUIET)
            ToHLoggingUtils.log(plugin, format, args);
        else
            ToHMessageUtils.broadcastMessage(plugin, format, args);
    }

    private void rankChange(final CommandSender sender, final String playerName, final String trackName, final boolean rankUp, final BroadcastScope scope, final boolean verbose) {
        uuidResolver.resolveUsername(sender, playerName, false, new CommandUuidResolverHandler() {
            @Override
            public void process(CommandSender sender, String name, UUID uuid, boolean group) {
                rankChange(sender, uuid, name, trackName, rankUp, scope, verbose);
            }
        });
    }

    // Perform the actual promotion/demotion
    private void rankChange(final CommandSender sender, final UUID uuid, final String playerName, String trackName, final boolean rankUp, final BroadcastScope scope, final boolean verbose) {
        // Resolve track
        final TrackMetaData trackMetaData = getTrack(sender, rankUp ? "promote" : "demote", trackName);
        if (trackMetaData == null)
            return;
        final List<String> track = trackMetaData.getTrack();

        // Do everything in one ginormous transaction.
        storageStrategy.getTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                Set<String> playerGroupNames = new HashSet<>();
                playerGroupNames.addAll(Utils.toGroupNames(Utils.filterExpired(storageStrategy.getDao().getGroups(uuid))));
                if (playerGroupNames.isEmpty())
                    playerGroupNames.add(resolver.getDefaultGroup());
        
                // Determine what groups the player and the track have in common
                playerGroupNames = getCommonGroups(playerGroupNames, track);
                
                if (playerGroupNames.size() > 1) {
                    // Hmm, player is member of 2 or more groups in track. Don't know
                    // what to do, so abort.
                    sendMessage(sender, colorize("{RED}Player is in more than one group in that track: {DARK_GREEN}%s"), delimitedString(", ", playerGroupNames));
                    abortBatchProcessing();
                    return false;
                }
                else if (playerGroupNames.isEmpty()) {
                    // Player not in any group. Only valid for rankUp
                    if (rankUp) {
                        String group = track.get(0);
                        try {
                            storageStrategy.getDao().addMember(group, uuid, playerName, null);
                        }
                        catch (MissingGroupException e) {
                            sendMessage(sender, colorize("{RED}Group {DARK_GREEN}%s{RED} does not exist."), e.getGroupName());
                            abortBatchProcessing();
                            return false;
                        }
                        announce("promote", scope, "%s added %s to %s", sender.getName(), playerName, group);
                        if (scope.isShouldEcho() || verbose)
                            sendMessage(sender, colorize("{YELLOW}Adding {AQUA}%s{YELLOW} to {DARK_GREEN}%s"), playerName, group);
                        fireRankEvent(playerName, trackMetaData.getTrackName(), null, group);
                    }
                    else {
                        sendMessage(sender, colorize("{RED}Player is not in any groups in that track."));
                        abortBatchProcessing();
                    }
                    return true;
                }
                else {
                    String oldGroup = playerGroupNames.iterator().next();
                    int rankIndex = track.indexOf(oldGroup);
                    assertFalse(rankIndex < 0); // should never happen...
        
                    // Rank up or down
                    rankIndex += rankUp ? 1 : -1;
        
                    // If now ranked below first rank, remove altogether
                    if (rankIndex < 0) {
                        storageStrategy.getDao().removeMember(oldGroup, uuid);
                        announce(rankUp ? "promote" : "demote", scope, "%s removed %s from %s", sender.getName(), playerName, oldGroup);
                        if (scope.isShouldEcho() || verbose)
                            sendMessage(sender, colorize("{YELLOW}Removing {AQUA}%s{YELLOW} from {DARK_GREEN}%s"), playerName, oldGroup);
                        fireRankEvent(playerName, trackMetaData.getTrackName(), oldGroup, null);
                    }
                    else {
                        // Constrain rank to [1..track.size() - 1]
                        if (rankIndex >= track.size()) rankIndex = track.size() - 1;
        
                        String newGroup = track.get(rankIndex);
        
                        // Change groups
                        try {
                            storageStrategy.getDao().addMember(newGroup, uuid, playerName, null);
                        }
                        catch (MissingGroupException e) {
                            sendMessage(sender, colorize("{RED}Group {DARK_GREEN}%s{RED} does not exist."), e.getGroupName());
                            abortBatchProcessing();
                            return false;
                        }
                        if (!oldGroup.equalsIgnoreCase(newGroup))
                            storageStrategy.getDao().removeMember(oldGroup, uuid);
        
                        announce(rankUp ? "promote" : "demote", scope, "%s %s %s from %s to %s", sender.getName(),
                                (rankUp ? "promoted" : "demoted"),
                                playerName,
                                oldGroup,
                                newGroup);
                        if (scope.isShouldEcho() || verbose)
                            sendMessage(sender, colorize("{YELLOW}%s {AQUA}%s{YELLOW} from {DARK_GREEN}%s{YELLOW} to {DARK_GREEN}%s"),
                                    (rankUp ? "Promoting" : "Demoting"),
                                    playerName,
                                    oldGroup,
                                    newGroup);
                        fireRankEvent(playerName, trackMetaData.getTrackName(), oldGroup, newGroup);
                    }
                    
                    return false;
                }
            }
        });
        
        core.invalidateMetadataCache(playerName, uuid, false);
        core.refreshPlayer(uuid, RefreshCause.GROUP_CHANGE);
        core.refreshExpirations(uuid);
    }

    private BroadcastScope determineScope(boolean quiet, boolean loud) {
        if (!(quiet || loud))
            return BroadcastScope.DEFAULT;
        else if (quiet && loud)
            return BroadcastScope.QUIET_LOUD;
        else if (quiet)
            return BroadcastScope.QUIET;
        else
            return BroadcastScope.LOUD;
    }

    @Command("promote")
    @Require("zpermissions.promote")
    public void promote(CommandSender sender, @Option("-q") boolean quiet, @Option("-Q") boolean loud, @Option("-v") boolean verbose, @Option(value="player", completer="player") String playerName, @Option(value="track", optional=true, completer="track") String trackName) {
        rankChange(sender, playerName, trackName, true, determineScope(quiet, loud), verbose);
    }

    @Command("demote")
    @Require("zpermissions.demote")
    public void demote(CommandSender sender, @Option("-q") boolean quiet, @Option("-Q") boolean loud, @Option("-v") boolean verbose,  @Option(value="player", completer="player") String playerName, @Option(value="track", optional=true, completer="track") String trackName) {
        rankChange(sender, playerName, trackName, false, determineScope(quiet, loud), verbose);
    }

    private void rankSet(final CommandSender sender, final String playerName, final String trackName, final String rankName, final BroadcastScope scope, final boolean verbose) {
        uuidResolver.resolveUsername(sender, playerName, false, new CommandUuidResolverHandler() {
            @Override
            public void process(CommandSender sender, String name, UUID uuid, boolean group) {
                rankSet(sender, uuid, name, trackName, rankName, scope, verbose);
            }
        });
    }

    // Set rank to a specified rank on a track
    private void rankSet(final CommandSender sender, final UUID uuid, final String playerName, String trackName, final String rankName, final BroadcastScope scope, final boolean verbose) {
        // Resolve track
        final TrackMetaData trackMetaData = getTrack(sender, rankName == null ? "unsetrank" : "setrank", trackName);
        if (trackMetaData == null)
            return;
        final List<String> track = trackMetaData.getTrack();

        if (rankName != null) {
            boolean found = false;
            for (String rank : track) {
                if (rank.equalsIgnoreCase(rankName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                sendMessage(sender, colorize("{RED}Rank is not in the track."));
                abortBatchProcessing();
                return;
            }
        }

        // Do everything in one ginormous transaction.
        storageStrategy.getTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                Set<String> playerGroupNames = new HashSet<>();
                playerGroupNames.addAll(Utils.toGroupNames(Utils.filterExpired(storageStrategy.getDao().getGroups(uuid))));
                if (playerGroupNames.isEmpty())
                    playerGroupNames.add(resolver.getDefaultGroup());
        
                // Determine what groups the player and the track have in common
                playerGroupNames = getCommonGroups(playerGroupNames, track);
                
                if (playerGroupNames.size() > 1) {
                    // Hmm, player is member of 2 or more groups in track. Don't know
                    // what to do, so abort.
                    sendMessage(sender, colorize("{RED}Player is in more than one group in that track: {DARK_GREEN}%s"), delimitedString(", ", playerGroupNames));
                    abortBatchProcessing();
                    return false;
                }
                else if (playerGroupNames.isEmpty()) {
                    if (rankName != null) {
                        // Not in any groups, just add to new group.
                        try {
                            storageStrategy.getDao().addMember(rankName, uuid, playerName, null);
                        }
                        catch (MissingGroupException e) {
                            sendMessage(sender, colorize("{RED}Group {DARK_GREEN}%s{RED} does not exist."), e.getGroupName());
                            abortBatchProcessing();
                            return false;
                        }
                        announce("setrank", scope, "%s added %s to %s", sender.getName(), playerName, rankName);
                        if (scope.isShouldEcho() || verbose)
                            sendMessage(sender, colorize("{YELLOW}Adding {AQUA}%s{YELLOW} to {DARK_GREEN}%s"), playerName, rankName);
                        fireRankEvent(playerName, trackMetaData.getTrackName(), null, rankName);
                    }
                    else {
                        sendMessage(sender, colorize("{RED}Player is not in any groups in that track."));
                        abortBatchProcessing();
                    }
                    return true;
                }
                else {
                    // Name of current (old) group
                    String oldGroup = playerGroupNames.iterator().next();

                    if (rankName != null) {
                        // Add to new group
                        try {
                            storageStrategy.getDao().addMember(rankName, uuid, playerName, null);
                        }
                        catch (MissingGroupException e) {
                            sendMessage(sender, colorize("{RED}Group {DARK_GREEN}%s{RED} does not exist."), e.getGroupName());
                            abortBatchProcessing();
                            return false;
                        }

                        // Remove from old group
                        if (!oldGroup.equalsIgnoreCase(rankName))
                            storageStrategy.getDao().removeMember(oldGroup, uuid);

                        announce("setrank", scope, "%s changed rank of %s from %s to %s", sender.getName(),
                                playerName,
                                oldGroup,
                                rankName);
                        if (scope.isShouldEcho() || verbose)
                            sendMessage(sender, colorize("{YELLOW}Changing rank of {AQUA}%s{YELLOW} from {DARK_GREEN}%s{YELLOW} to {DARK_GREEN}%s"),
                                    playerName,
                                    oldGroup,
                                    rankName);
                        fireRankEvent(playerName, trackMetaData.getTrackName(), oldGroup, rankName);
                    }
                    else {
                        // Remove from old group
                        storageStrategy.getDao().removeMember(oldGroup, uuid);

                        announce("unsetrank", scope, "%s removed %s from %s", sender.getName(), playerName, oldGroup);
                        if (scope.isShouldEcho() || verbose)
                            sendMessage(sender, colorize("{YELLOW}Removing {AQUA}%s{YELLOW} from {DARK_GREEN}%s"), playerName, oldGroup);
                        fireRankEvent(playerName, trackMetaData.getTrackName(), oldGroup, null);
                    }
                    return false;
                }
            }
        });

        core.invalidateMetadataCache(playerName, uuid, false);
        core.refreshPlayer(uuid, RefreshCause.GROUP_CHANGE);
        core.refreshExpirations(uuid);
    }

    @Command("setrank")
    @Require("zpermissions.setrank")
    public void setrank(CommandSender sender, @Option("-q") boolean quiet, @Option("-Q") boolean loud, @Option("-v") boolean verbose, @Option(value="player", completer="player") String playerName, @Option("rank") String rankName, @Option(value="track", optional=true, completer="track") String trackName) {
        rankSet(sender, playerName, trackName, rankName, determineScope(quiet, loud), verbose);
    }

    @Command("unsetrank")
    @Require("zpermissions.unsetrank")
    public void unsetrank(CommandSender sender, @Option("-q") boolean quiet, @Option("-Q") boolean loud, @Option("-v") boolean verbose, @Option(value="player", completer="player") String playerName, @Option(value="track", optional=true, completer="track") String trackName) {
        rankSet(sender, playerName, trackName, null, determineScope(quiet, loud), verbose);
    }

    // Returns names of tracks this permissible has access to
    private Set<String> getAvailableTracks(CommandSender sender, String command) {
        final String prefix = "zpermissions." + command + ".";
        Set<String> result = new HashSet<>();
        for (String track : config.getTracks()) {
            boolean found = false;
            for (String perm : new String[] { prefix + track, prefix + "*", "zpermissions.rank." + track, "zpermissions.rank.*" }) {
                if (sender.hasPermission(perm)) {
                    found = true; // continue search (for negations)
                }
                else if (sender.isPermissionSet(perm)) {
                    found = false; // explicit negation
                    break;
                }
            }
            if (found)
                result.add(track);
        }
        return result;
    }

    // Returns ranks for the specified track, determining default
    // accordingly. Returns null if invalid.
    private TrackMetaData getTrack(CommandSender sender, String command, String trackName) {
        if (!hasText(trackName)) { // Use default track
            // Figure out what player has access to
            Set<String> playerTracks = getAvailableTracks(sender, command);
            if (playerTracks.size() == 1) {
                // Exactly one choice, use it
                trackName = playerTracks.iterator().next();
            }
            else {
                // Fall back to configured default
                trackName = config.getDefaultTrack();
            }
            sendMessage(sender, colorize("{GRAY}(Defaulting to track \"%s\")"), trackName);
        }
        
        final List<String> track = config.getTrack(trackName);
        if (track == null || track.isEmpty()) {
            sendMessage(sender, colorize("{RED}Track has not been defined."));
            abortBatchProcessing();
            return null;
        }

        // TODO Permission checked twice in some cases. Any way around it?
        requireOnePermission(sender, true,
                String.format("zpermissions.%s.%s", command, trackName),
                String.format("zpermissions.%s.*", command),
                String.format("zpermissions.rank.%s", trackName),
                "zpermissions.rank.*");

        return new TrackMetaData(track, trackName);
    }

    private void fireRankEvent(String playerName, String track, String fromGroup, String toGroup) {
        ZPermissionsRankChangeEvent event = new ZPermissionsRankChangeEvent(playerName, track, fromGroup, toGroup);
        Bukkit.getPluginManager().callEvent(event);
    }

    private Set<String> getCommonGroups(Collection<String> currentGroups, Collection<String> trackGroups) {
        Set<String> result = new LinkedHashSet<>(currentGroups.size());
        // Lowercase all group names
        for (String group : currentGroups) {
            result.add(group.toLowerCase());
        }
        result.retainAll(trackGroups);
        return result;
    }

    private static class TrackMetaData {
        
        private final List<String> track;
        
        private final String trackName;

        public TrackMetaData(List<String> track, String trackName) {
            this.track = track;
            this.trackName = trackName;
        }

        public List<String> getTrack() {
            return track;
        }

        public String getTrackName() {
            return trackName;
        }
        
    }

}
