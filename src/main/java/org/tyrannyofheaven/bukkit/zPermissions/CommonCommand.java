package org.tyrannyofheaven.bukkit.zPermissions;

import static org.tyrannyofheaven.bukkit.util.ToHUtils.sendMessage;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Session;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;

public abstract class CommonCommand {

    private final boolean group;
    
    protected CommonCommand(boolean group) {
        this.group = group;
    }

    @Command("get")
    public void get(final ZPermissionsPlugin plugin, CommandSender sender, final @Session("entityName") String name, @Option("permission") String permission) {
        final WorldPermission wp = new WorldPermission(permission);
    
        Boolean result = plugin.getTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                return plugin.getDao().getPermission(name, group, wp.getWorld(), wp.getPermission());
            }
        });
        
        sendMessage(sender, "%s%s = %s", ChatColor.YELLOW, permission, result);
    }

    @Command("set")
    public void set(final ZPermissionsPlugin plugin, CommandSender sender, final @Session("entityName") String name, @Option("permission") String permission, final @Option(value="value", optional=true) Boolean value) {
        final WorldPermission wp = new WorldPermission(permission);
    
        plugin.getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                plugin.getDao().setPermission(name, group, wp.getWorld(), wp.getPermission(), value == null ? Boolean.TRUE : value);
            }
        });
    
        sendMessage(sender, "%s%s set to %s", ChatColor.YELLOW, permission, value == null ? Boolean.TRUE : value);
        plugin.refreshPlayers();
    }

    @Command( { "unset", "rm" })
    public void unset(final ZPermissionsPlugin plugin, CommandSender sender, final @Session("entityName") String name, @Option("permission") String permission) {
        final WorldPermission wp = new WorldPermission(permission);
    
        plugin.getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                plugin.getDao().unsetPermission(name, group, wp.getWorld(), wp.getPermission());
            }
        });
    
        sendMessage(sender, "%s%s unset", ChatColor.YELLOW, permission);
        plugin.refreshPlayers();
    }

}
