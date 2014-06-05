package org.tyrannyofheaven.bukkit.zPermissions.vault;

import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.hasText;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.PermissionsResolver;
import org.tyrannyofheaven.bukkit.zPermissions.QualifiedPermission;
import org.tyrannyofheaven.bukkit.zPermissions.RefreshCause;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsConfig;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsCore;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService;
import org.tyrannyofheaven.bukkit.zPermissions.dao.MissingGroupException;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.storage.StorageStrategy;

import com.google.common.base.Joiner;

// Current as of Permission.java f01cc6b89106bdb850a4e3d0be1425541b665712
public class VaultPermissionBridge extends PermissionCompatibility implements Listener {

    private final StorageStrategy storageStrategy;

    private final ZPermissionsCore core;

    private final ZPermissionsService service;

    private final ZPermissionsConfig config;

    public VaultPermissionBridge(Plugin plugin, PermissionsResolver resolver, StorageStrategy storageStrategy, ZPermissionsCore core, ZPermissionsService service, ZPermissionsConfig config) {
        super(resolver);
        this.plugin = plugin;
        this.storageStrategy = storageStrategy;
        this.core = core;
        this.service = service;
        this.config = config;
    }

    @Override
    public String[] getGroups() {
        Set<String> result = service.getAllGroups();
        return result.toArray(new String[result.size()]);
    }

    @Override
    public String getName() {
        return "zPermissions";
    }

    @Override
    public String[] getPlayerGroups(String world, OfflinePlayer player) {
        Collection<String> result;
        if (config.isVaultGetGroupsUsesAssignedOnly())
            result = service.getPlayerAssignedGroups(player.getUniqueId());
        else
            result = service.getPlayerGroups(player.getUniqueId());
        return result.toArray(new String[result.size()]);
    }

    @Override
    public String getPrimaryGroup(String world, OfflinePlayer player) {
        return service.getPlayerPrimaryGroup(player.getUniqueId());
    }

    @Override
    public boolean groupAdd(String world, final String group, final String permission) {
        if (!hasText(world))
            world = null;
        if (!hasText(group) || !hasText(permission)) {
            complainInvalidArguments();
            return false;
        }

        final String permWorld = world;
        try {
            getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
                @Override
                public void doInTransactionWithoutResult() throws Exception {
                    getDao().setPermission(group, null, true, null, permWorld, permission, true);
                }
            });
            core.refreshAffectedPlayers(group);
            core.logExternalChange("Added permission '%s' to group %s via Vault",
                    new QualifiedPermission(null, world, permission), group);
            return true;
        }
        catch (MissingGroupException e) {
            return false;
        }
    }

    @Override
    public boolean groupHas(String world, String group, String permission) {
        Map<String, Boolean> perms = service.getGroupPermissions(world, null, group);
        Boolean value = perms.get(permission.toLowerCase());
        if (value != null) {
            return value;
        }
        // Use default, if possible
        org.bukkit.permissions.Permission perm = Bukkit.getPluginManager().getPermission(permission);
        if (perm != null) {
            return perm.getDefault().getValue(false); // OP flag assumed to be false...
        }
        // Who knows...
        return false;
    }

    @Override
    public boolean groupRemove(String world, final String group, final String permission) {
        if (!hasText(world))
            world = null;
        if (!hasText(group) || !hasText(permission)) {
            complainInvalidArguments();
            return false;
        }

        final String permWorld = world;
        boolean result = getTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                return getDao().unsetPermission(group, null, true, null, permWorld, permission);
            }
        });
        if (result) {
            core.refreshAffectedPlayers(group);
            core.logExternalChange("Removed permission '%s' from group %s via Vault",
                    new QualifiedPermission(null, world, permission), group);
        }
        return result;
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }

    @Override
    public boolean hasSuperPermsCompat() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean playerAdd(String world, final OfflinePlayer player, final String permission) {
        if (!hasText(world))
            world = null;
        if (player == null || !hasText(permission)) {
            complainInvalidArguments();
            return false;
        }

        final UUID uuid = player.getUniqueId();
        final String playerName = player.getName();

        final String permWorld = world;
        getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                getDao().setPermission(playerName, uuid, false, null, permWorld, permission, true);
            }
        });
        core.refreshPlayer(uuid, RefreshCause.COMMAND);
        core.logExternalChange("Added permission '%s' to player %s via Vault",
                new QualifiedPermission(null, world, permission), playerName);
        return true;
    }

    @Override
    public boolean playerAddGroup(String world, final OfflinePlayer player, final String group) {
        if (player == null || !hasText(group)) {
            complainInvalidArguments();
            return false;
        }

        final UUID uuid = player.getUniqueId();
        final String playerName = player.getName();

        // NB world ignored
        try {
            getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
                @Override
                public void doInTransactionWithoutResult() throws Exception {
                    getDao().addMember(group, uuid, playerName, null);
                }
            });
        }
        catch (MissingGroupException e) {
            return false;
        }
        core.invalidateMetadataCache(playerName, uuid, false);
        core.refreshPlayer(uuid, RefreshCause.GROUP_CHANGE);
        core.logExternalChange("Added player %s to group %s via Vault",
                playerName, group);
        return true;
    }

    @Override
    public boolean playerHas(String world, OfflinePlayer player, String permission) {
        if (!player.isOnline()) {
            Map<String, Boolean> perms = service.getPlayerPermissions(world, null, player.getUniqueId());
            Boolean value = perms.get(permission.toLowerCase());
            if (value != null) {
                return value;
            }
            // Use default at this point
            org.bukkit.permissions.Permission perm = Bukkit.getPluginManager().getPermission(permission);
            if (perm != null) {
                return perm.getDefault().getValue(player.isOp());
            }
            // Have no clue
            return false;
        }
        else {
            return playerHas((Player)player, permission);
        }
    }

    @Override
    public boolean playerInGroup(String world, OfflinePlayer player, String group) {
        Collection<String> groups;
        if (config.isVaultGroupTestUsesAssignedOnly())
            groups = service.getPlayerAssignedGroups(player.getUniqueId());
        else
            groups = service.getPlayerGroups(player.getUniqueId());
        // Groups are case-insensitive...
        for (String g : groups) {
            if (g.equalsIgnoreCase(group)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean playerRemove(String world, final OfflinePlayer player, final String permission) {
        if (!hasText(world))
            world = null;
        if (player == null || !hasText(permission)) {
            complainInvalidArguments();
            return false;
        }

        final UUID uuid = player.getUniqueId();
        final String playerName = player.getName();

        final String permWorld = world;
        boolean result = getTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                return getDao().unsetPermission(playerName, uuid, false, null, permWorld, permission);
            }
        });
        if (result) {
            core.refreshPlayer(uuid, RefreshCause.COMMAND);
            core.logExternalChange("Removed permission '%s' from player %s via Vault",
                    new QualifiedPermission(null, world, permission), playerName);
        }
        return result;
    }

    @Override
    public boolean playerRemoveGroup(String world, final OfflinePlayer player, final String group) {
        if (player == null || !hasText(group)) {
            complainInvalidArguments();
            return false;
        }

        final UUID uuid = player.getUniqueId();
        final String playerName = player.getName();

        // NB world ignored
        try {
            getTransactionStrategy().execute(new TransactionCallback<Boolean>() {
                @Override
                public Boolean doInTransaction() throws Exception {
                    return getDao().removeMember(group, uuid);
                }
            });
        }
        catch (MissingGroupException e) {
            return false;
        }
        core.invalidateMetadataCache(playerName, uuid, false);
        core.refreshPlayer(uuid, RefreshCause.GROUP_CHANGE);
        core.logExternalChange("Removed player %s from group %s via Vault",
                playerName, group);
        return true;
    }

    public void register() {
        Bukkit.getServicesManager().register(Permission.class, this, plugin, ServicePriority.Highest);
    }

    private PermissionDao getDao() {
        return storageStrategy.getDao();
    }

    private TransactionStrategy getTransactionStrategy() {
        return storageStrategy.getRetryingTransactionStrategy();
    }

    private void complainInvalidArguments() {
        if (plugin.getLogger().isLoggable(Level.CONFIG)) {
            StackTraceElement[] ste = (new Throwable()).getStackTrace();
            plugin.getLogger().config("Vault method called with invalid arguments:\n        at " + Joiner.on("\n        at ").join(ste));
        }
    }

}
