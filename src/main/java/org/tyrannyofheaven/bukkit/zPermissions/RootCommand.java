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

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.log;
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
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.HelpBuilder;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Require;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;

/**
 * Handler for top-level commands:
 * <ul>
 *   <li>/permissions</li>
 *   <li>/promote</li>
 *   <li>/demote</li>
 *   <li>/setrank</li>
 * </ul>
 * 
 * @author zerothangel
 */
public class RootCommand {

    // Parent plugin
    private final ZPermissionsPlugin plugin;

    // Handler for /permissions sub-commands
    private final SubCommands sc;

    RootCommand(ZPermissionsPlugin plugin) {
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

    // Perform the actual promotion/demotion
    private void rankChange(final CommandSender sender, final String playerName, String trackName, final boolean rankUp) {
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
                String.format("zpermissions.%s.%s", rankUp ? "promote" : "demote", trackName));

        // Determine what groups the player and the track have in common
        final Set<String> trackGroupNames = new HashSet<String>(track);

        // Do everything in one ginormous transaction.
        plugin.getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                Set<String> playerGroupNames = new HashSet<String>();
                playerGroupNames.addAll(plugin.getDao().getGroups(playerName));
        
                playerGroupNames.retainAll(trackGroupNames);
                
                if (playerGroupNames.size() > 1) {
                    // Hmm, player is member of 2 or more groups in track. Don't know
                    // what to do, so abort.
                    sendMessage(sender, colorize("{RED}Player is in more than one group in that track: {DARK_GREEN}%s"), delimitedString(", ", playerGroupNames));
                    return;
                }
                else if (playerGroupNames.isEmpty()) {
                    // Player not in any group. Only valid for rankUp
                    if (rankUp) {
                        String group = track.get(0);
                        plugin.getDao().addMember(group, playerName);
                        log(plugin, "%s added %s to %s", sender.getName(), playerName, group);
                        sendMessage(sender, colorize("{YELLOW}Adding {AQUA}%s{YELLOW} to {DARK_GREEN}%s"), playerName, group);
                    }
                    else {
                        sendMessage(sender, colorize("{RED}Player is not in any groups in that track."));
                        return;
                    }
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
                        log(plugin, "%s removed %s from %s", sender.getName(), playerName, oldGroup);
                        sendMessage(sender, colorize("{YELLOW}Removing {AQUA}%s{YELLOW} from {DARK_GREEN}%s"), playerName, oldGroup);
                    }
                    else {
                        // Constrain rank to [1..track.size() - 1]
                        if (rankIndex >= track.size()) rankIndex = track.size() - 1;
        
                        String newGroup = track.get(rankIndex);
        
                        // Change groups
                        plugin.getDao().removeMember(oldGroup, playerName);
                        plugin.getDao().addMember(newGroup, playerName);
        
                        log(plugin, "%s %s %s from %s to %s", sender.getName(),
                                (rankUp ? "promoted" : "demoted"),
                                playerName,
                                oldGroup,
                                newGroup);
                        sendMessage(sender, colorize("{YELLOW}%s {AQUA}%s{YELLOW} from {DARK_GREEN}%s{YELLOW} to {DARK_GREEN}%s"),
                                (rankUp ? "Promoting" : "Demoting"),
                                playerName,
                                oldGroup,
                                newGroup);
                    }
                }
            }
        });
        
        plugin.checkPlayer(sender, playerName);
        plugin.refreshPlayer(playerName);
    }

    @Command("promote")
    @Require("zpermissions.promote")
    public void promote(CommandSender sender, @Option("player") String playerName, @Option(value="track", optional=true) String trackName) {
        rankChange(sender, playerName, trackName, true);
    }

    @Command("demote")
    @Require("zpermissions.demote")
    public void demote(CommandSender sender, @Option("player") String playerName, @Option(value="track", optional=true) String trackName) {
        rankChange(sender, playerName, trackName, false);
    }

    @Command("setrank")
    @Require("zpermissions.setrank")
    public void setrank(final CommandSender sender, @Option("player") final String playerName, @Option("rank") final String rankName, @Option(value="track", optional=true) String trackName) {
        // TODO lots of duped code, refactor

        // Resolve track
        if (!hasText(trackName))
            trackName = plugin.getDefaultTrack();
        
        final List<String> track = plugin.getTrack(trackName);
        if (track == null || track.isEmpty()) {
            sendMessage(sender, colorize("{RED}Track has not been defined."));
            return;
        }

        requireOnePermission(sender,
                "zpermissions.setrank.*",
                String.format("zpermissions.setrank.%s", trackName));

        if (!track.contains(rankName)) {
            sendMessage(sender, colorize("{RED}Rank is not in the track."));
            return;
        }

        // Determine what groups the player and the track have in common
        final Set<String> trackGroupNames = new HashSet<String>(track);

        // Do everything in one ginormous transaction.
        plugin.getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                Set<String> playerGroupNames = new HashSet<String>();
                playerGroupNames.addAll(plugin.getDao().getGroups(playerName));
        
                playerGroupNames.retainAll(trackGroupNames);
                
                if (playerGroupNames.size() > 1) {
                    // Hmm, player is member of 2 or more groups in track. Don't know
                    // what to do, so abort.
                    sendMessage(sender, colorize("{RED}Player is in more than one group in that track: {DARK_GREEN}%s"), delimitedString(", ", playerGroupNames));
                    return;
                }
                else if (playerGroupNames.isEmpty()) {
                    // Not in any groups, just add to new group.
                    plugin.getDao().addMember(rankName, playerName);
                    log(plugin, "%s added %s to %s", sender.getName(), playerName, rankName);
                    sendMessage(sender, colorize("{YELLOW}Adding {AQUA}%s{YELLOW} to {DARK_GREEN}%s"), playerName, rankName);
                }
                else {
                    // Remove from old group
                    String oldGroup = playerGroupNames.iterator().next();

                    // Change groups
                    plugin.getDao().removeMember(oldGroup, playerName);
                    plugin.getDao().addMember(rankName, playerName);
    
                    log(plugin, "%s changed rank of %s from %s to %s", sender.getName(),
                            playerName,
                            oldGroup,
                            rankName);
                    sendMessage(sender, colorize("{YELLOW}Changing rank of {AQUA}%s{YELLOW} from {DARK_GREEN}%s{YELLOW} to {DARK_GREEN}%s"),
                            playerName,
                            oldGroup,
                            rankName);
                }
            }
        });
    }

}
