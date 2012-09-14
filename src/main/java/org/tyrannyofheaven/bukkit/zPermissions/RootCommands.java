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
package org.tyrannyofheaven.bukkit.zPermissions;

import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;
import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.delimitedString;
import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.hasText;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.assertFalse;
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
 * @author asaddi
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

    RootCommands(ZPermissionsPlugin plugin) {
        this.plugin = plugin;
        sc = new SubCommands(plugin);
    }

    @Command("permissions")
    @Require({"zpermissions.player", "zpermissions.group", "zpermissions.list", "zpermissions.check", "zpermissions.reload",
        "zpermissions.import", "zpermissions.export"})
    public Object perm(HelpBuilder helpBuilder, CommandSender sender, String[] args) {
        if (args.length == 0) {
            helpBuilder.withCommandSender(sender)
                .withHandler(sc)
                .forCommand("player")
                .forCommand("group")
                .forCommand("list")
                .forCommand("check")
                .forCommand("reload")
                .forCommand("import")
                .forCommand("export")
                .show();
            return null;
        }
        return sc;
    }

    // Audit record of change. If quiet, only log to server log rather than
    // broadcasting to admins.
    private void announce(BroadcastScope scope, String format, Object... args) {
        if (scope == BroadcastScope.DEFAULT)
            ToHMessageUtils.broadcastAdmin(plugin, format, args);
        else if (scope == BroadcastScope.QUIET)
            ToHLoggingUtils.log(plugin, format, args);
        else
            ToHMessageUtils.broadcastMessage(plugin, format, args);
    }

    // Perform the actual promotion/demotion
    private void rankChange(final CommandSender sender, final String playerName, String trackName, final boolean rankUp, final BroadcastScope scope) {
        // Resolve track
        if (!hasText(trackName))
            trackName = plugin.getDefaultTrack();
        
        final List<String> track = plugin.getTrack(trackName);
        if (track == null || track.isEmpty()) {
            sendMessage(sender, colorize("{RED}Track has not been defined."));
            return;
        }

        requireOnePermission(sender,
                String.format("zpermissions.%s.*", rankUp ? "promote" : "demote"),
                String.format("zpermissions.%s.%s", rankUp ? "promote" : "demote", trackName),
                "zpermissions.rank.*",
                String.format("zpermissions.rank.%s", trackName));

        // Determine what groups the player and the track have in common
        final Set<String> trackGroupNames = new HashSet<String>(track);

        // Do everything in one ginormous transaction.
        Boolean check = plugin.getTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                Set<String> playerGroupNames = new HashSet<String>();
                playerGroupNames.addAll(plugin.getDao().getGroups(playerName));
        
                playerGroupNames.retainAll(trackGroupNames);
                
                if (playerGroupNames.size() > 1) {
                    // Hmm, player is member of 2 or more groups in track. Don't know
                    // what to do, so abort.
                    sendMessage(sender, colorize("{RED}Player is in more than one group in that track: {DARK_GREEN}%s"), delimitedString(", ", playerGroupNames));
                    return false;
                }
                else if (playerGroupNames.isEmpty()) {
                    // Player not in any group. Only valid for rankUp
                    if (rankUp) {
                        String group = track.get(0);
                        try {
                            plugin.getDao().addMember(group, playerName);
                        }
                        catch (MissingGroupException e) {
                            sendMessage(sender, colorize("{RED}Group {DARK_GREEN}%s{RED} does not exist."), e.getGroupName());
                            return false;
                        }
                        announce(scope, "%s added %s to %s", sender.getName(), playerName, group);
                        if (scope.isShouldEcho())
                            sendMessage(sender, colorize("{YELLOW}Adding {AQUA}%s{YELLOW} to {DARK_GREEN}%s"), playerName, group);
                    }
                    else {
                        sendMessage(sender, colorize("{RED}Player is not in any groups in that track."));
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
                        announce(scope, "%s removed %s from %s", sender.getName(), playerName, oldGroup);
                        if (scope.isShouldEcho())
                            sendMessage(sender, colorize("{YELLOW}Removing {AQUA}%s{YELLOW} from {DARK_GREEN}%s"), playerName, oldGroup);
                    }
                    else {
                        // Constrain rank to [1..track.size() - 1]
                        if (rankIndex >= track.size()) rankIndex = track.size() - 1;
        
                        String newGroup = track.get(rankIndex);
        
                        // Change groups
                        plugin.getDao().removeMember(oldGroup, playerName);
                        plugin.getDao().addMember(newGroup, playerName);
        
                        announce(scope, "%s %s %s from %s to %s", sender.getName(),
                                (rankUp ? "promoted" : "demoted"),
                                playerName,
                                oldGroup,
                                newGroup);
                        if (scope.isShouldEcho())
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
        
        if (check && scope.isShouldEcho())
            plugin.checkPlayer(sender, playerName);
        plugin.refreshPlayer(playerName);
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
    public void promote(CommandSender sender, @Option("-q") boolean quiet, @Option("-Q") boolean loud, @Option("player") String playerName, @Option(value="track", optional=true) String trackName) {
        rankChange(sender, playerName, trackName, true, determineScope(quiet, loud));
    }

    @Command("demote")
    @Require("zpermissions.demote")
    public void demote(CommandSender sender, @Option("-q") boolean quiet, @Option("-Q") boolean loud, @Option("player") String playerName, @Option(value="track", optional=true) String trackName) {
        rankChange(sender, playerName, trackName, false, determineScope(quiet, loud));
    }

    // Set rank to a specified rank on a track
    private void rankSet(final CommandSender sender, final String playerName, String trackName, final String rankName, final BroadcastScope scope) {
        // TODO lots of duped code from rankChange, refactor

        // Resolve track
        if (!hasText(trackName))
            trackName = plugin.getDefaultTrack();
        
        final List<String> track = plugin.getTrack(trackName);
        if (track == null || track.isEmpty()) {
            sendMessage(sender, colorize("{RED}Track has not been defined."));
            return;
        }

        requireOnePermission(sender,
                String.format("zpermissions.%s.*", rankName == null ? "unsetrank" : "setrank"),
                String.format("zpermissions.%s.%s", (rankName == null ? "unsetrank" : "setrank"), trackName),
                "zpermissions.rank.*",
                String.format("zpermissions.rank.%s", trackName));

        if (rankName != null && !track.contains(rankName)) {
            sendMessage(sender, colorize("{RED}Rank is not in the track."));
            return;
        }

        // Determine what groups the player and the track have in common
        final Set<String> trackGroupNames = new HashSet<String>(track);

        // Do everything in one ginormous transaction.
        Boolean check = plugin.getTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                Set<String> playerGroupNames = new HashSet<String>();
                playerGroupNames.addAll(plugin.getDao().getGroups(playerName));
        
                playerGroupNames.retainAll(trackGroupNames);
                
                if (playerGroupNames.size() > 1) {
                    // Hmm, player is member of 2 or more groups in track. Don't know
                    // what to do, so abort.
                    sendMessage(sender, colorize("{RED}Player is in more than one group in that track: {DARK_GREEN}%s"), delimitedString(", ", playerGroupNames));
                    return false;
                }
                else if (playerGroupNames.isEmpty()) {
                    if (rankName != null) {
                        // Not in any groups, just add to new group.
                        try {
                            plugin.getDao().addMember(rankName, playerName);
                        }
                        catch (MissingGroupException e) {
                            sendMessage(sender, colorize("{RED}Group {DARK_GREEN}%s{RED} does not exist."), e.getGroupName());
                            return false;
                        }
                        announce(scope, "%s added %s to %s", sender.getName(), playerName, rankName);
                        if (scope.isShouldEcho())
                            sendMessage(sender, colorize("{YELLOW}Adding {AQUA}%s{YELLOW} to {DARK_GREEN}%s"), playerName, rankName);
                    }
                    else {
                        sendMessage(sender, colorize("{RED}Player is not in any groups in that track."));
                    }
                    return true;
                }
                else {
                    // Remove from old group
                    String oldGroup = playerGroupNames.iterator().next();
                    plugin.getDao().removeMember(oldGroup, playerName);

                    if (rankName != null) {
                        // Add to new group
                        plugin.getDao().addMember(rankName, playerName);

                        announce(scope, "%s changed rank of %s from %s to %s", sender.getName(),
                                playerName,
                                oldGroup,
                                rankName);
                        if (scope.isShouldEcho())
                            sendMessage(sender, colorize("{YELLOW}Changing rank of {AQUA}%s{YELLOW} from {DARK_GREEN}%s{YELLOW} to {DARK_GREEN}%s"),
                                    playerName,
                                    oldGroup,
                                    rankName);
                    }
                    else {
                        announce(scope, "%s removed %s from %s", sender.getName(), playerName, oldGroup);
                        if (scope.isShouldEcho())
                            sendMessage(sender, colorize("{YELLOW}Removing {AQUA}%s{YELLOW} from {DARK_GREEN}%s"), playerName, oldGroup);
                    }
                    return false;
                }
            }
        });

        if (check && scope.isShouldEcho())
            plugin.checkPlayer(sender, playerName);
        plugin.refreshPlayer(playerName);
    }

    @Command("setrank")
    @Require("zpermissions.setrank")
    public void setrank(CommandSender sender, @Option("-q") boolean quiet, @Option("-Q") boolean loud, @Option("player") String playerName, @Option("rank") String rankName, @Option(value="track", optional=true) String trackName) {
        rankSet(sender, playerName, trackName, rankName, determineScope(quiet, loud));
    }

    @Command("unsetrank")
    @Require("zpermissions.unsetrank")
    public void unsetrank(CommandSender sender, @Option("-q") boolean quiet, @Option("-Q") boolean loud, @Option("player") String playerName, @Option(value="track", optional=true) String trackName) {
        rankSet(sender, playerName, trackName, null, determineScope(quiet, loud));
    }

}
