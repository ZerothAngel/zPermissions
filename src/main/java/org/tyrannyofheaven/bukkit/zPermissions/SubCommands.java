package org.tyrannyofheaven.bukkit.zPermissions;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.ParseException;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

public class SubCommands {

    @Command("get")
    public void get(ZPermissionsPlugin plugin, @Option("player") String name, @Option("permission") String permission, CommandSender sender) {
        ToHUtils.sendMessage(sender, "%s = %s", permission, plugin.getDao().getPermission(name, false, permission));
    }

    @Command("set")
    public void set(ZPermissionsPlugin plugin, @Option("player") String name, @Option("permission") String permission, @Option("value") boolean value) {
        plugin.getDao().setPermission(name, false, permission, value);
        Player player = plugin.getServer().getPlayer(name);
        if (player != null)
            plugin.addAttachment(player);
    }

    @Command({"unset", "rm"})
    public void unset(ZPermissionsPlugin plugin, @Option("player") String name, @Option("permission") String permission) {
        plugin.getDao().unsetPermission(name, false, permission);
    }

    @Command("addmember")
    public void addMember(ZPermissionsPlugin plugin, @Option("group") String groupName, @Option("player") String playerName) {
        plugin.getDao().addMember(groupName, playerName);
        Player player = plugin.getServer().getPlayer(playerName);
        if (player != null)
            plugin.addAttachment(player);
    }

    @Command({"removemember", "rmmember"})
    public void removeMember(ZPermissionsPlugin plugin, @Option("group") String groupName, @Option("player") String playerName) {
        plugin.getDao().removeMember(groupName, playerName);
        Player player = plugin.getServer().getPlayer(playerName);
        if (player != null)
            plugin.addAttachment(player);
    }

    @Command("groups")
    public void getGroups(ZPermissionsPlugin plugin, CommandSender sender, @Option("player") String name) {
        Set<PermissionEntity> groups;
        plugin.getDatabase().beginTransaction();
        try {
            groups = plugin.getDao().getGroups(name);
        }
        finally {
            plugin.getDatabase().endTransaction();
        }
        if (groups.isEmpty()) {
            sender.sendMessage("Not a member of any group");
        }
        else {
            StringBuilder sb = new StringBuilder();
            for (Iterator<PermissionEntity> i = groups.iterator(); i.hasNext();) {
                PermissionEntity group = i.next();
                sb.append(group.getName());
                if (i.hasNext())
                    sb.append(", ");
            }
            sender.sendMessage(ChatColor.YELLOW + sb.toString());
        }
    }

    @Command("setgroup")
    public void setGroup(ZPermissionsPlugin plugin, @Option("player") String playerName, @Option("group") String groupName) {
        plugin.getDao().setGroup(playerName, groupName);
        Player player = plugin.getServer().getPlayer(playerName);
        if (player != null)
            plugin.addAttachment(player);
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

}
