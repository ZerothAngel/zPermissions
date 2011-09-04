package org.tyrannyofheaven.bukkit.zPermissions;

import org.bukkit.command.CommandSender;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Session;

class GroupCommand {

    @Command("get")
    public void get(ZPermissionsPlugin plugin, CommandSender sender, @Session("groupName") String name, @Option("permission") String permission) {
        ToHUtils.sendMessage(sender, "%s = %s", permission, plugin.getDao().getPermission(name, true, permission));
    }

    @Command("set")
    public void set(ZPermissionsPlugin plugin, CommandSender sender, @Session("groupName") String name, @Option("permission") String permission, @Option("value") boolean value) {
        plugin.getDao().setPermission(name, true, permission, value);
        ToHUtils.sendMessage(sender, "%s set to %s", permission, value);
        // TODO refresh group members or everything
    }

    @Command({"unset", "rm"})
    public void unset(ZPermissionsPlugin plugin, CommandSender sender, @Session("groupName") String name, @Option("permission") String permission) {
        plugin.getDao().unsetPermission(name, true, permission);
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

}
