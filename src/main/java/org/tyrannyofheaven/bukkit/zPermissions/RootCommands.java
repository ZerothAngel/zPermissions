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
import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.delimitedString;
import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.hasText;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.assertFalse;
import static org.tyrannyofheaven.bukkit.util.command.reader.CommandReader.abortBatchProcessing;
import static org.tyrannyofheaven.bukkit.util.permissions.PermissionUtils.requireOnePermission;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.tyrannyofheaven.bukkit.util.ToHLoggingUtils;
import org.tyrannyofheaven.bukkit.util.ToHMessageUtils;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.HelpBuilder;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Require;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.zPermissions.dao.MissingGroupException;

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
 * @author zerothangel
 */
public class RootCommands {

    // Parent plugin
    private final ZPermissionsPlugin plugin;

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

    RootCommands(ZPermissionsPlugin plugin, PermissionsResolver resolver) {
        this.plugin = plugin;
        sc = new SubCommands(plugin, resolver);
    }

    @Command("permissions")
    @Require({"zpermissions.player", "zpermissions.group", "zpermissions.list", "zpermissions.check", "zpermissions.reload",
        "zpermissions.import", "zpermissions.export", "zpermissions.inspect", "zpermissions.mygroups"})
    public Object perm(HelpBuilder helpBuilder, CommandSender sender, String[] args) {
        if (args.length == 0) {
            helpBuilder.withCommandSender(sender)
                .withHandler(sc)
                .forCommand("player")
                .forCommand("group")
                .forCommand("list")
                .forCommand("check")
                .forCommand("inspect")
                .forCommand("mygroups")
                .forCommand("reload")
                .forCommand("import")
                .forCommand("export")
                .forCommand("refresh")
                .show();
            return null;
        }
        return sc;
    }

    // Audit record of change. If quiet, only log to server log rather than
    // broadcasting to admins.
    private void announce(String notifyNode, BroadcastScope scope, String format, Object... args) {
        if (scope == BroadcastScope.DEFAULT) {
            if (plugin.isRankAdminBroadcast()) {
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

    // Perform the actual promotion/demotion
    private void rankChange(final CommandSender sender, final String playerName, String trackName, final boolean rankUp, final BroadcastScope scope, final boolean verbose) {
        // Resolve track
        if (!hasText(trackName))
            trackName = plugin.getDefaultTrack();
        
        final List<String> track = plugin.getTrack(trackName);
        if (track == null || track.isEmpty()) {
            sendMessage(sender, colorize("{RED}Track has not been defined."));
            abortBatchProcessing();
            return;
        }

        requireOnePermission(sender, true,
                String.format("zpermissions.%s.%s", rankUp ? "promote" : "demote", trackName),
                String.format("zpermissions.%s.*", rankUp ? "promote" : "demote"),
                String.format("zpermissions.rank.%s", trackName),
                "zpermissions.rank.*");

        // Determine what groups the player and the track have in common
        final Set<String> trackGroupNames = new HashSet<String>(track);

        // Do everything in one ginormous transaction.
        Boolean check = plugin.getTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                Set<String> playerGroupNames = new HashSet<String>();
                playerGroupNames.addAll(Utils.toGroupNames(Utils.filterExpired(plugin.getDao().getGroups(playerName))));
                if (playerGroupNames.isEmpty())
                    playerGroupNames.add(plugin.getResolver().getDefaultGroup());
        
                playerGroupNames.retainAll(trackGroupNames);
                
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
                            plugin.getDao().addMember(group, playerName, null);
                        }
                        catch (MissingGroupException e) {
                            sendMessage(sender, colorize("{RED}Group {DARK_GREEN}%s{RED} does not exist."), e.getGroupName());
                            abortBatchProcessing();
                            return false;
                        }
                        announce("promote", scope, "%s added %s to %s", sender.getName(), playerName, group);
                        if (scope.isShouldEcho() || verbose)
                            sendMessage(sender, colorize("{YELLOW}Adding {AQUA}%s{YELLOW} to {DARK_GREEN}%s"), playerName, group);
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
                        plugin.getDao().removeMember(oldGroup, playerName);
                        announce(rankUp ? "promote" : "demote", scope, "%s removed %s from %s", sender.getName(), playerName, oldGroup);
                        if (scope.isShouldEcho() || verbose)
                            sendMessage(sender, colorize("{YELLOW}Removing {AQUA}%s{YELLOW} from {DARK_GREEN}%s"), playerName, oldGroup);
                    }
                    else {
                        // Constrain rank to [1..track.size() - 1]
                        if (rankIndex >= track.size()) rankIndex = track.size() - 1;
        
                        String newGroup = track.get(rankIndex);
        
                        // Change groups
                        try {
                            plugin.getDao().addMember(newGroup, playerName, null);
                        }
                        catch (MissingGroupException e) {
                            sendMessage(sender, colorize("{RED}Group {DARK_GREEN}%s{RED} does not exist."), e.getGroupName());
                            abortBatchProcessing();
                            return false;
                        }
                        if (!oldGroup.equalsIgnoreCase(newGroup))
                            plugin.getDao().removeMember(oldGroup, playerName);
        
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
                    }
                    
                    return false;
                }
            }
        });
        
        if (check && (scope.isShouldEcho() || verbose))
            plugin.checkPlayer(sender, playerName);
        plugin.refreshPlayer(playerName);
        plugin.refreshExpirations();
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

    // Set rank to a specified rank on a track
    private void rankSet(final CommandSender sender, final String playerName, String trackName, final String rankName, final BroadcastScope scope, final boolean verbose) {
        // TODO lots of duped code from rankChange, refactor

        // Resolve track
        if (!hasText(trackName))
            trackName = plugin.getDefaultTrack();
        
        final List<String> track = plugin.getTrack(trackName);
        if (track == null || track.isEmpty()) {
            sendMessage(sender, colorize("{RED}Track has not been defined."));
            abortBatchProcessing();
            return;
        }

        requireOnePermission(sender, true,
                String.format("zpermissions.%s.%s", (rankName == null ? "unsetrank" : "setrank"), trackName),
                String.format("zpermissions.%s.*", rankName == null ? "unsetrank" : "setrank"),
                String.format("zpermissions.rank.%s", trackName),
                "zpermissions.rank.*");

        if (rankName != null && !track.contains(rankName)) {
            sendMessage(sender, colorize("{RED}Rank is not in the track."));
            abortBatchProcessing();
            return;
        }

        // Determine what groups the player and the track have in common
        final Set<String> trackGroupNames = new HashSet<String>(track);

        // Do everything in one ginormous transaction.
        Boolean check = plugin.getTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                Set<String> playerGroupNames = new HashSet<String>();
                playerGroupNames.addAll(Utils.toGroupNames(Utils.filterExpired(plugin.getDao().getGroups(playerName))));
                if (playerGroupNames.isEmpty())
                    playerGroupNames.add(plugin.getResolver().getDefaultGroup());
        
                playerGroupNames.retainAll(trackGroupNames);
                
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
                            plugin.getDao().addMember(rankName, playerName, null);
                        }
                        catch (MissingGroupException e) {
                            sendMessage(sender, colorize("{RED}Group {DARK_GREEN}%s{RED} does not exist."), e.getGroupName());
                            abortBatchProcessing();
                            return false;
                        }
                        announce("setrank", scope, "%s added %s to %s", sender.getName(), playerName, rankName);
                        if (scope.isShouldEcho() || verbose)
                            sendMessage(sender, colorize("{YELLOW}Adding {AQUA}%s{YELLOW} to {DARK_GREEN}%s"), playerName, rankName);
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
                            plugin.getDao().addMember(rankName, playerName, null);
                        }
                        catch (MissingGroupException e) {
                            sendMessage(sender, colorize("{RED}Group {DARK_GREEN}%s{RED} does not exist."), e.getGroupName());
                            abortBatchProcessing();
                            return false;
                        }

                        // Remove from old group
                        if (!oldGroup.equalsIgnoreCase(rankName))
                            plugin.getDao().removeMember(oldGroup, playerName);

                        announce("setrank", scope, "%s changed rank of %s from %s to %s", sender.getName(),
                                playerName,
                                oldGroup,
                                rankName);
                        if (scope.isShouldEcho() || verbose)
                            sendMessage(sender, colorize("{YELLOW}Changing rank of {AQUA}%s{YELLOW} from {DARK_GREEN}%s{YELLOW} to {DARK_GREEN}%s"),
                                    playerName,
                                    oldGroup,
                                    rankName);
                    }
                    else {
                        // Remove from old group
                        plugin.getDao().removeMember(oldGroup, playerName);

                        announce("unsetrank", scope, "%s removed %s from %s", sender.getName(), playerName, oldGroup);
                        if (scope.isShouldEcho() || verbose)
                            sendMessage(sender, colorize("{YELLOW}Removing {AQUA}%s{YELLOW} from {DARK_GREEN}%s"), playerName, oldGroup);
                    }
                    return false;
                }
            }
        });

        if (check && (scope.isShouldEcho() || verbose))
            plugin.checkPlayer(sender, playerName);
        plugin.refreshPlayer(playerName);
        plugin.refreshExpirations();
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

}
