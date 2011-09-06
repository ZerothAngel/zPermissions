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

import static org.tyrannyofheaven.bukkit.util.ToHUtils.assertFalse;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.delimitedString;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.hasText;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.sendMessage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.HelpBuilder;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Require;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

public class RootCommand {

    private final SubCommands sc = new SubCommands();

    @Command("permissions")
    @Require({"zpermissions.player", "zpermissions.group", "zpermissions.list", "zpermissions.check", "zpermissions.reload"})
    public Object perm(HelpBuilder helpBuilder, CommandSender sender, String[] args) {
        if (args.length == 0) {
            helpBuilder.withCommandSender(sender)
                .withHandler(sc)
                .forCommand("player")
                .forCommand("group")
                .forCommand("list")
                .forCommand("check")
                .forCommand("reload")
                .show();
            return null;
        }
        return sc;
    }

    private void rankChange(final ZPermissionsPlugin plugin, final CommandSender sender, final String playerName, String trackName, final boolean rankUp) {
        // Resolve track
        if (!hasText(trackName))
            trackName = plugin.getDefaultTrack();
        
        final List<String> track = plugin.getTrack(trackName);
        if (track == null || track.isEmpty()) {
            sendMessage(sender, colorize("{RED}Track has not been defined."));
            return;
        }
        
        // Determine what groups the player and the track have in common
        final Set<String> trackGroupNames = new HashSet<String>(track);

        // Do everything in one ginormous transaction. TODO move the sendMessage
        // calls outside of transaction...
        plugin.getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                Set<String> playerGroupNames = new HashSet<String>();
                for (PermissionEntity group : plugin.getDao().getGroups(playerName)) {
                    playerGroupNames.add(group.getName());
                }
        
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
                        sendMessage(sender, colorize("{YELLOW}Removing {AQUA}%s{YELLOW} from {DARK_GREEN}%s"), playerName, oldGroup);
                    }
                    else {
                        // Constrain rank to [1..track.size() - 1]
                        if (rankIndex >= track.size()) rankIndex = track.size() - 1;
        
                        String newGroup = track.get(rankIndex);
        
                        // Change groups
                        plugin.getDao().removeMember(oldGroup, playerName);
                        plugin.getDao().addMember(newGroup, playerName);
        
                        sendMessage(sender, colorize("{YELLOW}Ranking %s {AQUA}%s{YELLOW} from {DARK_GREEN}%s{YELLOW} to {DARK_GREEN}%s"),
                                (rankUp ? "up" : "down"),
                                playerName,
                                oldGroup,
                                newGroup);
                    }
                }
            }
        });
        
        plugin.refreshPlayer(playerName);
    }

    @Command("promote")
    @Require("zpermissions.promote")
    public void promote(ZPermissionsPlugin plugin, CommandSender sender, @Option("player") String playerName, @Option(value="track", optional=true) String trackName) {
        rankChange(plugin, sender, playerName, trackName, true);
    }

    @Command("demote")
    @Require("zpermissions.demote")
    public void demote(ZPermissionsPlugin plugin, CommandSender sender, @Option("player") String playerName, @Option(value="track", optional=true) String trackName) {
        rankChange(plugin, sender, playerName, trackName, false);
    }

}
