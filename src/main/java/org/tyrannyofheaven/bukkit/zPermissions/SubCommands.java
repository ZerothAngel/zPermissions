package org.tyrannyofheaven.bukkit.zPermissions;

import static org.tyrannyofheaven.bukkit.util.ToHUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.sendMessage;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.CommandSession;
import org.tyrannyofheaven.bukkit.util.command.HelpBuilder;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.ParseException;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

public class SubCommands {

    private final PlayerCommand playerCommand = new PlayerCommand();

    private final CommonCommand groupCommand = new GroupCommand();

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
        
        session.setValue("entityName", playerName);
        return playerCommand;
    }

    @Command({"group", "gr", "g"})
    public CommonCommand group(HelpBuilder helpBuilder, CommandSender sender, CommandSession session, @Option(value="group", nullable=true) String groupName, String[] args) {
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
        
        session.setValue("entityName", groupName);
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
            throw new ParseException("<what> should be 'groups' or 'players'");
        }

        List<PermissionEntity> entities = plugin.getDao().getEntities(group);

        if (entities.isEmpty()) {
            sendMessage(sender, colorize("{YELLOW}No %s found."), group ? "groups" : "players");
        }
        else {
            for (PermissionEntity entity : entities) {
                sendMessage(sender, colorize("{DARK_GREEN}- %s"), entity.getDisplayName());
            }
        }
    }

    @Command("check")
    public void check(ZPermissionsPlugin plugin, CommandSender sender, @Option("permission") String permission, @Option(value="player", optional=true) String playerName) {
        Player player;
        if (playerName == null) {
            if (!(sender instanceof Player)) {
                sendMessage(sender, colorize("{RED}Cannot check permissions of console."));
                return;
            }
            player = (Player)sender;
        }
        else {
            player = plugin.getServer().getPlayer(playerName);
            if (player == null) {
                sendMessage(sender, colorize("{RED}Player is not online."));
                return;
            }
        }
        
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            if (permission.equalsIgnoreCase(pai.getPermission())) {
                sendMessage(sender, colorize("{AQUA}%s{YELLOW} sets {GOLD}%s{YELLOW} to {GREEN}%s"), player.getName(), pai.getPermission(), pai.getValue());
                return;
            }
        }
        sendMessage(sender, colorize("{AQUA}%s{YELLOW} does not set {GOLD}%s"), player.getName(), permission);
    }

}
