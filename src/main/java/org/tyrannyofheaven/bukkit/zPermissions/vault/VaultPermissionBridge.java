package org.tyrannyofheaven.bukkit.zPermissions.vault;

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.debug;
import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.hasText;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

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
import org.tyrannyofheaven.bukkit.zPermissions.QualifiedPermission;
import org.tyrannyofheaven.bukkit.zPermissions.RefreshCause;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsConfig;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsCore;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService;
import org.tyrannyofheaven.bukkit.zPermissions.dao.MissingGroupException;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.storage.StorageStrategy;

import com.google.common.base.Joiner;

// Current as of Permission.java 73a0c8f5ab6d15033296c3833ea727bec453192c
public class VaultPermissionBridge extends Permission implements Listener {

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
                    getDao().setPermission(group, true, null, permWorld, permission, true);
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
                return getDao().unsetPermission(group, true, null, permWorld, permission);
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
    public boolean playerAdd(String world, final String player, final String permission) {
        if (!hasText(world))
            world = null;
        if (!hasText(player) || !hasText(permission)) {
            complainInvalidArguments();
            return false;
        }

        final String permWorld = world;
        getTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                getDao().setPermission(player, false, null, permWorld, permission, true);
            }
        });
        core.refreshPlayer(player, RefreshCause.COMMAND);
        core.logExternalChange("Added permission '%s' to player %s via Vault",
                new QualifiedPermission(null, world, permission), player);
        return true;
    }

    @Override
    public boolean playerAddGroup(String world, final String player, final String group) {
        if (!hasText(player) || !hasText(group)) {
            complainInvalidArguments();
            return false;
        }

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
        core.invalidateMetadataCache(player, false);
        core.refreshPlayer(player, RefreshCause.GROUP_CHANGE);
        core.logExternalChange("Added player %s to group %s via Vault",
                player, group);
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
    public boolean playerRemove(String world, final String player, final String permission) {
        if (!hasText(world))
            world = null;
        if (!hasText(player) || !hasText(permission)) {
            complainInvalidArguments();
            return false;
        }

        final String permWorld = world;
        boolean result = getTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                return getDao().unsetPermission(player, false, null, permWorld, permission);
            }
        });
        if (result) {
            core.refreshPlayer(player, RefreshCause.COMMAND);
            core.logExternalChange("Removed permission '%s' from player %s via Vault",
                    new QualifiedPermission(null, world, permission), player);
        }
        return result;
    }

    @Override
    public boolean playerRemoveGroup(String world, final String player, final String group) {
        if (!hasText(player) || !hasText(group)) {
            complainInvalidArguments();
            return false;
        }

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
        core.invalidateMetadataCache(player, false);
        core.refreshPlayer(player, RefreshCause.GROUP_CHANGE);
        core.logExternalChange("Removed player %s from group %s via Vault",
                player, group);
        return true;
    }

    // Following transient methods overridden for sole purpose of logging

    @Override
    public boolean playerAddTransient(Player player, String permission) {
        boolean result = super.playerAddTransient(player, permission);
        // Always true, but eh
        if (result) {
            core.logExternalChange("Added transient permission '%s' to player %s via Vault",
                    permission, player.getName());
        }
        return result;
    }

    @Override
    public boolean playerRemoveTransient(Player player, String permission) {
        boolean result = super.playerRemoveTransient(player, permission);
        if (result) {
            core.logExternalChange("Removed transient permission '%s' from player %s via Vault",
                    permission, player.getName());
        }
        return result;
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

    private void complainInvalidArguments() {
        if (plugin.getLogger().isLoggable(Level.CONFIG)) {
            StackTraceElement[] ste = (new Throwable()).getStackTrace();
            plugin.getLogger().config("Vault method called with invalid arguments:\n        at " + Joiner.on("\n        at ").join(ste));
        }
    }

}
