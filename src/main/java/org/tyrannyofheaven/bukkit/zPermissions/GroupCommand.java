package org.tyrannyofheaven.bukkit.zPermissions;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Session;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

public class GroupCommand {

    @Command("get")
    public void get(ZPermissionsPlugin plugin, CommandSender sender, @Session("groupName") String name, @Option("permission") String permission) {
        WorldPermission wp = new WorldPermission(permission);
        ToHUtils.sendMessage(sender, "%s = %s", permission, plugin.getDao().getPermission(name, true, wp.getWorld(), wp.getPermission()));
    }

    @Command("set")
    public void set(ZPermissionsPlugin plugin, CommandSender sender, @Session("groupName") String name, @Option("permission") String permission, @Option(value="value", optional=true) Boolean value) {
        WorldPermission wp = new WorldPermission(permission);
        if (value == null)
            value = Boolean.TRUE;
        plugin.getDao().setPermission(name, true, wp.getWorld(), wp.getPermission(), value);
        ToHUtils.sendMessage(sender, "%s set to %s", permission, value);
        // TODO refresh group members or everything
    }

    @Command({"unset", "rm"})
    public void unset(ZPermissionsPlugin plugin, CommandSender sender, @Session("groupName") String name, @Option("permission") String permission) {
        WorldPermission wp = new WorldPermission(permission);
        plugin.getDao().unsetPermission(name, true, wp.getWorld(), wp.getPermission());
        ToHUtils.sendMessage(sender, "%s unset", permission);
    }

    @Command("addmember")
    public void addMember(ZPermissionsPlugin plugin, @Session("groupName") String groupName, @Option("player") String playerName) {
        plugin.getDao().addMember(groupName, playerName);
        plugin.refreshPlayer(playerName);
    }

    @Command({"removemember", "rmmember"})
    public void removeMember(ZPermissionsPlugin plugin, @Session("groupName") String groupName, @Option("player") String playerName) {
        plugin.getDao().removeMember(groupName, playerName);
        plugin.refreshPlayer(playerName);
    }

    @Command("show")
    public void show(ZPermissionsPlugin plugin, CommandSender sender, @Session("groupName") String groupName) {
        plugin.getDatabase().beginTransaction();
        PermissionEntity entity;
        try {
            entity = plugin.getDao().getEntity(groupName, true);
        }
        finally {
            plugin.getDatabase().endTransaction();
        }
        if (entity != null) {
            ToHUtils.sendMessage(sender, "%sGroup-specific permissions for %s%s:", ChatColor.YELLOW, ChatColor.WHITE, entity.getDisplayName());
            if (entity.getParent() != null) {
                ToHUtils.sendMessage(sender, "%sParent: %s", ChatColor.DARK_BLUE, entity.getParent().getDisplayName());
            }
            for (Entry e : entity.getPermissions()) {
                ToHUtils.sendMessage(sender, "%s- %s%s: %s", ChatColor.DARK_GREEN, e.getWorld() == null ? "" : e.getWorld().getName() + ":", e.getPermission(), e.isValue());
            }
        }
        if (entity == null || entity.getPermissions().isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Group has no group-specific permissions");
            return;
        }
        
    }

    @Command("setparent")
    public void setParent(ZPermissionsPlugin plugin, CommandSender sender, @Session("groupName") String groupName, @Option(value="parent", optional=true) String parentName) {
        plugin.getDao().setParent(groupName, parentName);
    }
    
}
