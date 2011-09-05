package org.tyrannyofheaven.bukkit.zPermissions;

import java.util.Iterator;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Session;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

public class PlayerCommand extends CommonCommand {

    public PlayerCommand() {
        super(false);
    }

    @Command("groups")
    public void getGroups(ZPermissionsPlugin plugin, CommandSender sender, @Session("entityName") String name) {
        List<PermissionEntity> groups = plugin.getDao().getGroups(name);

        // Add default group if needed and available
        if (groups.isEmpty()) {
            PermissionEntity defaultGroup = plugin.getDao().getEntity(plugin.getDefaultGroup(), true);
            if (defaultGroup != null)
                groups.add(defaultGroup);
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
    public void setGroup(final ZPermissionsPlugin plugin, final @Session("entityName") String playerName, final @Option("group") String groupName) {
        plugin.getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                plugin.getDao().setGroup(playerName, groupName);
            }
        });
        plugin.refreshPlayer(playerName);
    }

    @Command("show")
    public void show(ZPermissionsPlugin plugin, CommandSender sender, @Session("entityName") String playerName) {
        PermissionEntity entity = plugin.getDao().getEntity(playerName, false);

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
