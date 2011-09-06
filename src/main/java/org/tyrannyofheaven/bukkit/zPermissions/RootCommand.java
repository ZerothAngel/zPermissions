package org.tyrannyofheaven.bukkit.zPermissions;

import static org.tyrannyofheaven.bukkit.util.ToHUtils.assertFalse;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.delimitedString;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.hasText;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.sendMessage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.HelpBuilder;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Require;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

public class RootCommand {

    private final SubCommands sc = new SubCommands();

    @Command("perm")
    @Require("zpermissions.*")
    public Object perm(HelpBuilder helpBuilder, CommandSender sender, String[] args) {
        if (args.length == 0) {
            helpBuilder.withCommandSender(sender)
                .withHandler(sc)
                .forCommand("player")
                .forCommand("group")
                .forCommand("list")
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
            sender.sendMessage(ChatColor.RED + "Track is not defined");
            return;
        }
        
        // Determine what groups the player and the track have in common
        final Set<String> trackGroupNames = new HashSet<String>(track);

        plugin.getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                // Do everything in one ginormous transaction. TODO move the sendMessage
                // calls outside of transaction...
                
                Set<String> playerGroupNames = new HashSet<String>();
                for (PermissionEntity group : plugin.getDao().getGroups(playerName)) {
                    playerGroupNames.add(group.getName());
                }
        
                playerGroupNames.retainAll(trackGroupNames);
                
                if (playerGroupNames.size() > 1) {
                    // Hmm, player is member of 2 or more groups in track. Don't know
                    // what to do, so abort.
                    sendMessage(sender, "%sPlayer is in more than one group in that track: %s%s", ChatColor.RED, ChatColor.WHITE, delimitedString(", ", playerGroupNames));
                    return;
                }
                else if (playerGroupNames.isEmpty()) {
                    // Player not in any group. Only valid for rankUp
                    if (rankUp) {
                        String group = track.get(0);
                        plugin.getDao().addMember(group, playerName);
                        sendMessage(sender, "%sAdding %s to %s%s", ChatColor.YELLOW, playerName, ChatColor.GREEN, group);
                    }
                    else {
                        sendMessage(sender, "%sPlayer is not in any groups in that track", ChatColor.RED);
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
                        sendMessage(sender, "%sRemoving %s from %s%s", ChatColor.YELLOW, playerName, ChatColor.GREEN, oldGroup);
                    }
                    else {
                        // Constrain rank to [1..track.size() - 1]
                        if (rankIndex >= track.size()) rankIndex = track.size() - 1;
        
                        String newGroup = track.get(rankIndex);
        
                        // Change groups
                        plugin.getDao().removeMember(oldGroup, playerName);
                        plugin.getDao().addMember(newGroup, playerName);
        
                        sendMessage(sender, "%sRanking %s %s from %s%s%s to %s%s",
                                ChatColor.YELLOW,
                                (rankUp ? "up" : "down"),
                                playerName,
                                ChatColor.GREEN, oldGroup, ChatColor.YELLOW,
                                ChatColor.GREEN, newGroup);
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
