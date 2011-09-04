package org.tyrannyofheaven.bukkit.zPermissions;

import java.util.Iterator;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Session;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

public class PlayerCommand {

    @Command("get")
    public void get(ZPermissionsPlugin plugin, CommandSender sender, @Session("playerName") String name, @Option("permission") String permission) {
        WorldPermission wp = new WorldPermission(permission);
        ToHUtils.sendMessage(sender, "%s = %s", permission, plugin.getDao().getPermission(name, false, wp.getWorld(), wp.getPermission()));
    }

    @Command("set")
    public void set(ZPermissionsPlugin plugin, CommandSender sender, @Session("playerName") String name, @Option("permission") String permission, @Option("value") boolean value) {
        WorldPermission wp = new WorldPermission(permission);
        plugin.getDao().setPermission(name, false, wp.getWorld(), wp.getPermission(), value);
        ToHUtils.sendMessage(sender, "%s set to %s", permission, value);
        plugin.refreshPlayer(name);
    }

    @Command({"unset", "rm"})
    public void unset(ZPermissionsPlugin plugin, CommandSender sender, @Session("playerName") String name, @Option("permission") String permission) {
        WorldPermission wp = new WorldPermission(permission);
        plugin.getDao().unsetPermission(name, false, wp.getWorld(), wp.getPermission());
        ToHUtils.sendMessage(sender, "%s unset", permission);
    }

    @Command("groups")
    public void getGroups(ZPermissionsPlugin plugin, CommandSender sender, @Session("playerName") String name) {
        Set<PermissionEntity> groups;
        plugin.getDatabase().beginTransaction();
        try {
            groups = plugin.getDao().getGroups(name);
            if (groups.isEmpty()) {
                PermissionEntity defaultGroup = plugin.getDao().getEntity(plugin.getDefaultGroup(), true);
                if (defaultGroup != null)
                    groups.add(defaultGroup);
            }
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
    public void setGroup(ZPermissionsPlugin plugin, @Session("playerName") String playerName, @Option("group") String groupName) {
        plugin.getDao().setGroup(playerName, groupName);
        plugin.refreshPlayer(playerName);
    }

}
