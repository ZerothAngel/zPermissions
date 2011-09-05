package org.tyrannyofheaven.bukkit.zPermissions;

import java.util.Iterator;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Session;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

public class PlayerCommand {

    @Command("get")
    public void get(ZPermissionsPlugin plugin, CommandSender sender, @Session("playerName") String name, @Option("permission") String permission) {
        WorldPermission wp = new WorldPermission(permission);
        ToHUtils.sendMessage(sender, "%s = %s", permission, plugin.getDao().getPermission(name, false, wp.getWorld(), wp.getPermission()));
    }

    @Command("set")
    public void set(ZPermissionsPlugin plugin, CommandSender sender, @Session("playerName") String name, @Option("permission") String permission, @Option(value="value", optional=true) Boolean value) {
        WorldPermission wp = new WorldPermission(permission);
        if (value == null)
            value = Boolean.TRUE;
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
        List<PermissionEntity> groups;
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

    @Command("show")
    public void show(ZPermissionsPlugin plugin, CommandSender sender, @Session("playerName") String playerName) {
        plugin.getDatabase().beginTransaction();
        PermissionEntity entity;
        try {
            entity = plugin.getDao().getEntity(playerName, false);
        }
        finally {
            plugin.getDatabase().endTransaction();
        }
        if (entity == null || entity.getPermissions().isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Player has no player-specific permissions");
            return;
        }
        
        ToHUtils.sendMessage(sender, "%sPlayer-specific permissions for %s%s:", ChatColor.YELLOW, ChatColor.WHITE, entity.getDisplayName());
        for (Entry e : entity.getPermissions()) {
            ToHUtils.sendMessage(sender, "%s- %s%s: %s", ChatColor.DARK_GREEN, e.getWorld() == null ? "" : e.getWorld().getName() + ":", e.getPermission(), e.isValue());
        }
    }

}
