package org.tyrannyofheaven.bukkit.zPermissions.vault;

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.debug;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import net.milkbowl.vault.permission.Permission;
import net.milkbowl.vault.permission.plugins.Permission_zPermissions;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.RefreshCause;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsConfig;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsCore;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService;
import org.tyrannyofheaven.bukkit.zPermissions.dao.MissingGroupException;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.storage.StorageStrategy;

// Current as of Permission.java 73a0c8f5ab6d15033296c3833ea727bec453192c
public class VaultPermissionBridge extends Permission implements Listener {

    private final Plugin plugin;

    private final StorageStrategy storageStrategy;

    private final ZPermissionsCore core;

    private final ZPermissionsService service;

    private final ZPermissionsConfig config;

    public VaultPermissionBridge(Plugin plugin, StorageStrategy storageStrategy, ZPermissionsCore core, ZPermissionsService service, ZPermissionsConfig config) {
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
    public String[] getPlayerGroups(String world, String player) {
        Collection<String> result;
        if (config.isVaultGetGroupsUsesAssignedOnly())
            result = service.getPlayerAssignedGroups(player);
        else
            result = service.getPlayerGroups(player);
        return result.toArray(new String[result.size()]);
    }

    @Override
    public String getPrimaryGroup(String world, String player) {
        return service.getPlayerPrimaryGroup(player);
    }

    @Override
    public boolean groupAdd(final String world, final String group, final String permission) {
        try {
            getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
                @Override
                public void doInTransactionWithoutResult() throws Exception {
                    getDao().setPermission(group, true, null, world, permission, true);
                }
            });
            core.refreshAffectedPlayers(group);
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
    public boolean groupRemove(final String world, final String group, final String permission) {
        boolean result = getTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                return getDao().unsetPermission(group, false, null, world, permission);
            }
        });
        if (result)
            core.refreshAffectedPlayers(group);
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
    public boolean playerAdd(final String world, final String player, final String permission) {
        getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                getDao().setPermission(player, false, null, world, permission, true);
            }
        });
        core.refreshPlayer(player, RefreshCause.COMMAND);
        return true;
    }

    @Override
    public boolean playerAddGroup(String world, final String player, final String group) {
        // NB world ignored
        try {
            getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
                @Override
                public void doInTransactionWithoutResult() throws Exception {
                    getDao().addMember(group, player, null);
                }
            });
        }
        catch (MissingGroupException e) {
            return false;
        }
        core.refreshPlayer(player, RefreshCause.GROUP_CHANGE);
        return true;
    }

    @Override
    public boolean playerHas(String world, String player, String permission) {
        Player p = Bukkit.getServer().getPlayer(player);
        if (p == null) {
            Map<String, Boolean> perms = service.getPlayerPermissions(world, null, player);
            Boolean value = perms.get(permission.toLowerCase());
            if (value != null) {
                return value;
            }
            // Use default at this point
            org.bukkit.permissions.Permission perm = Bukkit.getPluginManager().getPermission(permission);
            if (perm != null) {
                OfflinePlayer op = Bukkit.getServer().getOfflinePlayer(player);
                return perm.getDefault().getValue(op != null ? op.isOp() : false);
            }
            // Have no clue
            return false;
        }
        else {
            return playerHas(p, permission);
        }
    }

    @Override
    public boolean playerInGroup(String world, String player, String group) {
        Collection<String> groups;
        if (config.isVaultGroupTestUsesAssignedOnly())
            groups = service.getPlayerAssignedGroups(player);
        else
            groups = service.getPlayerGroups(player);
        // Groups are case-insensitive...
        for (String g : groups) {
            if (g.equalsIgnoreCase(group)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean playerRemove(final String world, final String player, final String permission) {
        boolean result = getTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                return getDao().unsetPermission(player, false, null, world, permission);
            }
        });
        if (result)
            core.refreshPlayer(player, RefreshCause.COMMAND);
        return result;
    }

    @Override
    public boolean playerRemoveGroup(String world, final String player, final String group) {
        // NB world ignored
        try {
            getTransactionStrategy().execute(new TransactionCallback<Boolean>() {
                @Override
                public Boolean doInTransaction() throws Exception {
                    return getDao().removeMember(group, player);
                }
            });
        }
        catch (MissingGroupException e) {
            return false;
        }
        core.refreshPlayer(player, RefreshCause.GROUP_CHANGE);
        return true;
    }

    public void register() {
        Bukkit.getServicesManager().register(Permission.class, this, plugin, ServicePriority.Highest);
        // To be removed once/if Vault 1.2.26 is released
        // In case Vault started before (though not really necessary if Bukkit's provider insert is stable)
        for (RegisteredServiceProvider<?> provider : Bukkit.getServicesManager().getRegistrations(Permission.class)) {
            removeIfDefaultVaultHandler(provider);
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // To be removed once/if Vault 1.2.26 is released
    @EventHandler
    public void vault_1_2_25_workaround(ServiceRegisterEvent event) {
        removeIfDefaultVaultHandler(event.getProvider());
    }

    private PermissionDao getDao() {
        return storageStrategy.getDao();
    }

    private TransactionStrategy getTransactionStrategy() {
        return storageStrategy.getRetryingTransactionStrategy();
    }

    // To be removed once/if Vault 1.2.26 is released
    private void removeIfDefaultVaultHandler(RegisteredServiceProvider<?> provider) {
        // This is necessary because I set the original handler in Vault to ServicePriority.Highest,
        // meaning I can't override it from zPermissions. This was fixed in post-1.2.25 Vault.
        if (Permission.class.equals(provider.getService()) &&
                "Vault".equals(provider.getPlugin().getName()) &&
                Permission_zPermissions.class.isAssignableFrom(provider.getProvider().getClass()) &&
                provider.getPriority() == ServicePriority.Highest) {
            debug(plugin, "There can be only one! Removing Vault's Permission handler for zPermissions");
            Bukkit.getServicesManager().unregister(Permission.class, provider.getProvider());
        }
    }

}
