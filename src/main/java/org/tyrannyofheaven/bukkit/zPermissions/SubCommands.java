package org.tyrannyofheaven.bukkit.zPermissions;

import java.util.Iterator;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

public class SubCommands {

    @Command("get")
    public void get(zPermissionsPlugin plugin, @Option("player") String name, @Option("permission") String permission, CommandSender sender) {
        ToHUtils.sendMessage(sender, "%s = %s", permission, plugin.getDao().getPermission(name, false, permission));
    }

    @Command("set")
    public void set(zPermissionsPlugin plugin, @Option("player") String name, @Option("permission") String permission, @Option("value") boolean value) {
        plugin.getDao().setPermission(name, false, permission, value);
    }

    @Command("rm")
    public void rm(zPermissionsPlugin plugin, @Option("player") String name, @Option("permission") String permission) {
        plugin.getDao().unsetPermission(name, false, permission);
    }

    @Command("addmember")
    public void addMember(zPermissionsPlugin plugin, @Option("group") String groupName, @Option("player") String playerName) {
        plugin.getDao().addMember(groupName, playerName);
    }

    @Command({"removemember", "rmmember"})
    public void removeMember(zPermissionsPlugin plugin, @Option("group") String groupName, @Option("player") String playerName) {
        plugin.getDao().removeMember(groupName, playerName);
    }

    @Command("groups")
    public void getGroups(zPermissionsPlugin plugin, CommandSender sender, @Option("player") String name) {
        Set<PermissionEntity> groups = plugin.getDao().getGroups(name);
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

}
