package org.tyrannyofheaven.bukkit.zPermissions;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.CommandSession;
import org.tyrannyofheaven.bukkit.util.command.HelpBuilder;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.ParseException;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

public class SubCommands {

    private final PlayerCommand playerCommand = new PlayerCommand();

    private final GroupCommand groupCommand = new GroupCommand();

    @Command({"player", "pl", "p"})
    public PlayerCommand player(HelpBuilder helpBuilder, CommandSender sender, CommandSession session, @Option(value="player", nullable=true) String playerName, String[] args) {
        if (args.length == 0) {
            helpBuilder.withCommandSender(sender)
                .withHandler(playerCommand)
                .forCommand("get")
                .forCommand("set")
                .forCommand("unset")
                .forCommand("groups")
                .forCommand("setgroup")
                .forCommand("show")
                .show();
            return null;
        }
        
        session.setValue("playerName", playerName);
        return playerCommand;
    }

    @Command({"group", "gr", "g"})
    public GroupCommand group(HelpBuilder helpBuilder, CommandSender sender, CommandSession session, @Option(value="group", nullable=true) String groupName, String[] args) {
        if (args.length == 0) {
            helpBuilder.withCommandSender(sender)
                .withHandler(groupCommand)
                .forCommand("get")
                .forCommand("set")
                .forCommand("unset")
                .forCommand("setparent")
                .forCommand("addmember")
                .forCommand("removemember")
                .forCommand("show")
                .show();
            return null;
        }
        
        session.setValue("groupName", groupName);
        return groupCommand;
    }

    @Command({"list", "ls"})
    public void list(ZPermissionsPlugin plugin, CommandSender sender, @Option("what") String what) {
        boolean group;
        if ("groups".startsWith(what)) {
            group = true;
        }
        else if ("players".startsWith(what)) {
            group = false;
        }
        else {
            throw new ParseException(ChatColor.RED + "<what> should be 'groups' or 'players'");
        }
        List<PermissionEntity> entities = plugin.getDao().getEntities(group);
        for (PermissionEntity entity : entities) {
            ToHUtils.sendMessage(sender, "%s- %s", ChatColor.DARK_GREEN, entity.getDisplayName());
        }
    }

    @Command("check")
    public void check(ZPermissionsPlugin plugin, CommandSender sender, @Option("permission") String permission, @Option(value="player", optional=true) String playerName) {
        Player player;
        if (playerName == null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Cannot check permissions of console");
                return;
            }
            player = (Player)sender;
        }
        else {
            player = plugin.getServer().getPlayer(playerName);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "No such player is online");
                return;
            }
        }
        
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            if (permission.equalsIgnoreCase(pai.getPermission())) {
                ToHUtils.sendMessage(sender, "%s%s = %s", ChatColor.GOLD, pai.getPermission(), pai.getValue());
                return;
            }
        }
        ToHUtils.sendMessage(sender, "%s%s does not set %s", ChatColor.GOLD, player.getName(), permission);
    }

}
