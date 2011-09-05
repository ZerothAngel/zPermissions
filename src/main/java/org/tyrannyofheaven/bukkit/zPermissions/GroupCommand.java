package org.tyrannyofheaven.bukkit.zPermissions;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Session;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

public class GroupCommand extends CommonCommand {

    public GroupCommand() {
        super(true);
    }

    @Command("addmember")
    public void addMember(final ZPermissionsPlugin plugin, final @Session("entityName") String groupName, final @Option("player") String playerName) {
        plugin.getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                plugin.getDao().addMember(groupName, playerName);
            }
        });

        plugin.refreshPlayer(playerName);
    }

    @Command({"removemember", "rmmember"})
    public void removeMember(final ZPermissionsPlugin plugin, final @Session("entityName") String groupName, final @Option("player") String playerName) {
        plugin.getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                plugin.getDao().removeMember(groupName, playerName);
            }
        });

        plugin.refreshPlayer(playerName);
    }

    @Command("show")
    public void show(ZPermissionsPlugin plugin, CommandSender sender, @Session("entityName") String groupName) {
        PermissionEntity entity = plugin.getDao().getEntity(groupName, true);

        if (entity != null) {
            ToHUtils.sendMessage(sender, "%sGroup-specific permissions for %s%s:", ChatColor.YELLOW, ChatColor.WHITE, entity.getDisplayName());
            ToHUtils.sendMessage(sender, "%sPriority: %s", ChatColor.YELLOW, entity.getPriority());
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
    public void setParent(final ZPermissionsPlugin plugin, CommandSender sender, final @Session("entityName") String groupName, final @Option(value="parent", optional=true) String parentName) {
        plugin.getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                plugin.getDao().setParent(groupName, parentName);
            }
        });

        plugin.refreshPlayers();
    }

    @Command("setpriority")
    public void setPriority(final ZPermissionsPlugin plugin, CommandSender sender, final @Session("entityName") String groupName, final @Option("priority") int priority) {
        plugin.getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                plugin.getDao().setPriority(groupName, priority);
            }
        });

        plugin.refreshPlayers();
    }

}
