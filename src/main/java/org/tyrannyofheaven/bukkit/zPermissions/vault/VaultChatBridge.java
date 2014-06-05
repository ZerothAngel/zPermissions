package org.tyrannyofheaven.bukkit.zPermissions.vault;

import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.hasText;

import java.util.UUID;
import java.util.logging.Level;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsConfig;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsCore;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService;
import org.tyrannyofheaven.bukkit.zPermissions.dao.MissingGroupException;
import org.tyrannyofheaven.bukkit.zPermissions.storage.StorageStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.util.MetadataConstants;

import com.google.common.base.Joiner;

// Current as of Chat.java f01cc6b89106bdb850a4e3d0be1425541b665712
public class VaultChatBridge extends ChatCompatibility {

    private final Plugin plugin;

    private final ZPermissionsCore core;

    private final StorageStrategy storageStrategy;

    private final ZPermissionsService service;

    private final ZPermissionsConfig config;

    public VaultChatBridge(Plugin plugin, ZPermissionsCore core, StorageStrategy storageStrategy, ZPermissionsService service, ZPermissionsConfig config) {
        super(Bukkit.getServicesManager().load(Permission.class));
        this.plugin = plugin;
        this.core = core;
        this.storageStrategy = storageStrategy;
        this.service = service;
        this.config = config;
    }

    @Override
    public boolean getGroupInfoBoolean(String world, String group, String node, boolean defaultValue) {
        Boolean result = service.getGroupMetadata(group, node, Boolean.class);
        if (result == null)
            return defaultValue;
        else
            return result;
    }

    @Override
    public double getGroupInfoDouble(String world, String group, String node, double defaultValue) {
        Double result = service.getGroupMetadata(group, node, Double.class);
        if (result == null)
            return defaultValue;
        else
            return result;
    }

    @Override
    public int getGroupInfoInteger(String world, String group, String node, int defaultValue) {
        Integer result = service.getGroupMetadata(group, node, Integer.class);
        if (result == null)
            return defaultValue;
        else
            return result;
    }

    @Override
    public String getGroupInfoString(String world, String group, String node, String defaultValue) {
        String result = service.getGroupMetadata(group, node, String.class);
        if (result == null)
            return defaultValue;
        else
            return result;
    }

    @Override
    public String getGroupPrefix(String world, String group) {
        return getGroupInfoString(world, group, MetadataConstants.PREFIX_KEY, "");
    }

    @Override
    public String getGroupSuffix(String world, String group) {
        return getGroupInfoString(world, group, MetadataConstants.SUFFIX_KEY, "");
    }

    @Override
    public String getName() {
        return "zPermissions";
    }

    @Override
    public boolean getPlayerInfoBoolean(String world, OfflinePlayer player, String node, boolean defaultValue) {
        Boolean result = service.getPlayerMetadata(player.getUniqueId(), node, Boolean.class);
        if (result == null && config.isVaultMetadataIncludesGroup())
            result = service.getGroupMetadata(getPrimaryGroup(world, player), node, Boolean.class);

        if (result == null)
            return defaultValue;
        else
            return result;
    }

    @Override
    public double getPlayerInfoDouble(String world, OfflinePlayer player, String node, double defaultValue) {
        Double result = service.getPlayerMetadata(player.getUniqueId(), node, Double.class);
        if (result == null && config.isVaultMetadataIncludesGroup())
            result = service.getGroupMetadata(getPrimaryGroup(world, player), node, Double.class);

        if (result == null)
            return defaultValue;
        else
            return result;
    }

    @Override
    public int getPlayerInfoInteger(String world, OfflinePlayer player, String node, int defaultValue) {
        Integer result = service.getPlayerMetadata(player.getUniqueId(), node, Integer.class);
        if (result == null && config.isVaultMetadataIncludesGroup())
            result = service.getGroupMetadata(getPrimaryGroup(world, player), node, Integer.class);

        if (result == null)
            return defaultValue;
        else
            return result;
    }

    @Override
    public String getPlayerInfoString(String world, OfflinePlayer player, String node, String defaultValue) {
        String result = service.getPlayerMetadata(player.getUniqueId(), node, String.class);
        if (result == null && config.isVaultMetadataIncludesGroup())
            result = service.getGroupMetadata(getPrimaryGroup(world, player), node, String.class);

        if (result == null)
            return defaultValue;
        else
            return result;
    }

    @Override
    public String getPlayerPrefix(String world, OfflinePlayer player) {
        return service.getPlayerPrefix(player.getUniqueId());
    }

    @Override
    public String getPlayerSuffix(String world, OfflinePlayer player) {
        return service.getPlayerSuffix(player.getUniqueId());
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public void register() {
        Bukkit.getServicesManager().register(Chat.class, this, plugin, ServicePriority.Highest);
    }

    @Override
    public void setGroupInfoBoolean(String world, String group, String node, boolean value) {
        set(group, null, true, node, value);
    }

    @Override
    public void setGroupInfoDouble(String world, String group, String node, double value) {
        set(group, null, true, node, value);
    }

    @Override
    public void setGroupInfoInteger(String world, String group, String node, int value) {
        set(group, null, true, node, value);
    }

    @Override
    public void setGroupInfoString(String world, String group, String node, String value) {
        set(group, null, true, node, value);
    }

    @Override
    public void setGroupPrefix(String world, String group, String prefix) {
        setGroupInfoString(world, group, MetadataConstants.PREFIX_KEY, prefix);
    }

    @Override
    public void setGroupSuffix(String world, String group, String suffix) {
        setGroupInfoString(world, group, MetadataConstants.SUFFIX_KEY, suffix);
    }

    @Override
    public void setPlayerInfoBoolean(String world, OfflinePlayer player, String node, boolean value) {
        set(player.getName(), player, false, node, value);
    }

    @Override
    public void setPlayerInfoDouble(String world, OfflinePlayer player, String node, double value) {
        set(player.getName(), player, false, node, value);
    }

    @Override
    public void setPlayerInfoInteger(String world, OfflinePlayer player, String node, int value) {
        set(player.getName(), player, false, node, value);
    }

    @Override
    public void setPlayerInfoString(String world, OfflinePlayer player, String node, String value) {
        set(player.getName(), player, false, node, value);
    }

    @Override
    public void setPlayerPrefix(String world, OfflinePlayer player, String prefix) {
        setPlayerInfoString(world, player, MetadataConstants.PREFIX_KEY, prefix);
    }

    @Override
    public void setPlayerSuffix(String world, OfflinePlayer player, String suffix) {
        setPlayerInfoString(world, player, MetadataConstants.SUFFIX_KEY, suffix);
    }

    private void set(final String name, OfflinePlayer player, final boolean group, final String metadataName, final Object value) {
        if (!hasText(name) || (!group && player == null) || !hasText(metadataName)) {
            complainInvalidArguments();
            return;
        }

        final UUID uuid;
        if (!group) {
            uuid = player.getUniqueId();
        }
        else uuid = null;

        try {
            storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
                @Override
                public void doInTransactionWithoutResult() throws Exception {
                    if (value != null)
                        storageStrategy.getDao().setMetadata(name, uuid, group, metadataName, value);
                    else
                        storageStrategy.getDao().unsetMetadata(name, uuid, group, metadataName);
                }
            });
            core.invalidateMetadataCache(name, uuid, group);
            core.logExternalChange("Metadata '%s' for %s %s set via Vault", metadataName,
                    group ? "group" : "player", name);
        }
        catch (MissingGroupException e) {
            // Ignore
        }
    }

    private void complainInvalidArguments() {
        plugin.getLogger().warning("Vault method called with invalid arguments. Broken plugin? Enable zPermissions debug to see stack trace.");
        if (plugin.getLogger().isLoggable(Level.CONFIG)) {
            StackTraceElement[] ste = (new Throwable()).getStackTrace();
            plugin.getLogger().config("Vault method called with invalid arguments:\n        at " + Joiner.on("\n        at ").join(ste));
        }
    }

}
