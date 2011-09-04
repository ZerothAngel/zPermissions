package org.tyrannyofheaven.bukkit.zPermissions;

import org.bukkit.command.CommandSender;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.Option;

public class SubCommands {

    @Command("dump")
    public void get(zPermissionsPlugin plugin, @Option("player") String name, @Option("permission") String permission, CommandSender sender) {
        ToHUtils.sendMessage(sender, "%s = %s", permission, plugin.getDao().getPlayerPermission(name, permission));
    }

    @Command("set")
    public void set(zPermissionsPlugin plugin, @Option("player") String name, @Option("permission") String permission, @Option("value") boolean value) {
        plugin.getDao().setPlayerPermission(name, permission, value);
    }

}
