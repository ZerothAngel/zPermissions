package org.tyrannyofheaven.bukkit.zPermissions.vault;

import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.hasText;

import java.util.logging.Level;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
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

// Current as of Chat.java 4305efde9212d556fafaa4dc78836f575a80ec91
public class VaultChatBridge extends Chat {

    private final Plugin plugin;

    private final ZPermissionsCore core;

    private final StorageStrategy storageStrategy;

    private final ZPermissionsService service;

    private final ZPermissionsConfig config;

    private final PlayerPrefixHandler prefixHandler;

    public VaultChatBridge(Plugin plugin, ZPermissionsCore core, StorageStrategy storageStrategy, ZPermissionsService service, ZPermissionsConfig config, PlayerPrefixHandler prefixHandler) {
        super(Bukkit.getServicesManager().load(Permission.class));
        this.plugin = plugin;
        this.core = core;
        this.storageStrategy = storageStrategy;
        this.service = service;
        this.config = config;
        this.prefixHandler = prefixHandler;
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
    public boolean getPlayerInfoBoolean(String world, String player, String node, boolean defaultValue) {
        Boolean result = service.getPlayerMetadata(player, node, Boolean.class);
        if (result == null && config.isVaultMetadataIncludesGroup())
            result = service.getGroupMetadata(getPrimaryGroup(world, player), node, Boolean.class);

        if (result == null)
            return defaultValue;
        else
            return result;
    }

    @Override
    public double getPlayerInfoDouble(String world, String player, String node, double defaultValue) {
        Double result = service.getPlayerMetadata(player, node, Double.class);
        if (result == null && config.isVaultMetadataIncludesGroup())
            result = service.getGroupMetadata(getPrimaryGroup(world, player), node, Double.class);

        if (result == null)
            return defaultValue;
        else
            return result;
    }

    @Override
    public int getPlayerInfoInteger(String world, String player, String node, int defaultValue) {
        Integer result = service.getPlayerMetadata(player, node, Integer.class);
        if (result == null && config.isVaultMetadataIncludesGroup())
            result = service.getGroupMetadata(getPrimaryGroup(world, player), node, Integer.class);

        if (result == null)
            return defaultValue;
        else
            return result;
    }

    @Override
    public String getPlayerInfoString(String world, String player, String node, String defaultValue) {
        String result = service.getPlayerMetadata(player, node, String.class);
        if (result == null && config.isVaultMetadataIncludesGroup())
            result = service.getGroupMetadata(getPrimaryGroup(world, player), node, String.class);

        if (result == null)
            return defaultValue;
        else
            return result;
    }

    @Override
    public String getPlayerPrefix(String world, String player) {
        return prefixHandler.getPlayerPrefix(player);
    }

    @Override
    public String getPlayerSuffix(String world, String player) {
        return prefixHandler.getPlayerSuffix(player);
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
        set(group, true, node, value);
    }

    @Override
    public void setGroupInfoDouble(String world, String group, String node, double value) {
        set(group, true, node, value);
    }

    @Override
    public void setGroupInfoInteger(String world, String group, String node, int value) {
        set(group, true, node, value);
    }

    @Override
    public void setGroupInfoString(String world, String group, String node, String value) {
        set(group, true, node, value);
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
    public void setPlayerInfoBoolean(String world, String player, String node, boolean value) {
        set(player, false, node, value);
    }

    @Override
    public void setPlayerInfoDouble(String world, String player, String node, double value) {
        set(player, false, node, value);
    }

    @Override
    public void setPlayerInfoInteger(String world, String player, String node, int value) {
        set(player, false, node, value);
    }

    @Override
    public void setPlayerInfoString(String world, String player, String node, String value) {
        set(player, false, node, value);
    }

    @Override
    public void setPlayerPrefix(String world, String player, String prefix) {
        setPlayerInfoString(world, player, MetadataConstants.PREFIX_KEY, prefix);
    }

    @Override
    public void setPlayerSuffix(String world, String player, String suffix) {
        setPlayerInfoString(world, player, MetadataConstants.SUFFIX_KEY, suffix);
    }

    private void set(final String name, final boolean group, final String metadataName, final Object value) {
        if (!hasText(name) || !hasText(metadataName)) {
            complainInvalidArguments();
            return;
        }

        try {
            storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
                @Override
                public void doInTransactionWithoutResult() throws Exception {
                    if (value != null)
                        storageStrategy.getDao().setMetadata(name, group, metadataName, value);
                    else
                        storageStrategy.getDao().unsetMetadata(name, group, metadataName);
                }
            });
            core.invalidateMetadataCache(name, group);
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
