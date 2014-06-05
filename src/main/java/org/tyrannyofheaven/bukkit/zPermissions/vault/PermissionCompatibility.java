package org.tyrannyofheaven.bukkit.zPermissions.vault;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.tyrannyofheaven.bukkit.zPermissions.PermissionsResolver;

// Implementation of all non-OfflinePlayer-based player methods
public abstract class PermissionCompatibility extends Permission {

    private final PermissionsResolver resolver;

    public PermissionCompatibility(PermissionsResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean playerHas(String world, String player, String permission) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
        if (offlinePlayer == null) return false;
        return playerHas(world, offlinePlayer, permission);
    }

    @Override
    public boolean playerAdd(String world, String player, String permission) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
        if (offlinePlayer == null) return false;
        return playerAdd(world, offlinePlayer, permission);
    }

    @Override
    public boolean playerRemove(String world, String player, String permission) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
        if (offlinePlayer == null) return false;
        return playerRemove(world, offlinePlayer, permission);
    }

    @Override
    public boolean playerInGroup(String world, String player, String group) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
        if (offlinePlayer == null) return false;
        return playerInGroup(world, offlinePlayer, group);
    }

    @Override
    public boolean playerAddGroup(String world, String player, String group) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
        if (offlinePlayer == null) return false;
        return playerAddGroup(world, offlinePlayer, group);
    }

    @Override
    public boolean playerRemoveGroup(String world, String player, String group) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
        if (offlinePlayer == null) return false;
        return playerRemoveGroup(world, offlinePlayer, group);
    }

    @Override
    public String[] getPlayerGroups(String world, String player) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
        if (offlinePlayer == null) return new String[] { resolver.getDefaultGroup() };
        return getPlayerGroups(world, offlinePlayer);
    }

    @Override
    public String getPrimaryGroup(String world, String player) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
        if (offlinePlayer == null) return resolver.getDefaultGroup();
        return getPrimaryGroup(world, offlinePlayer);
    }

}
