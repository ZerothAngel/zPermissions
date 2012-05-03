/*
 * Copyright 2011 Allan Saddi <allan@saddi.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tyrannyofheaven.bukkit.zPermissions;

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.debug;
import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.error;
import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.log;
import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.warn;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.broadcastAdmin;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;
import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.hasText;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.tyrannyofheaven.bukkit.util.ToHFileUtils;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.VersionInfo;
import org.tyrannyofheaven.bukkit.util.command.ToHCommandExecutor;
import org.tyrannyofheaven.bukkit.util.transaction.AvajeTransactionStrategy;
import org.tyrannyofheaven.bukkit.util.transaction.RetryingAvajeTransactionStrategy;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.dao.AvajePermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionRegion;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionWorld;
import org.tyrannyofheaven.bukkit.zPermissions.service.ZPermissionsServiceImpl;

import com.avaje.ebean.cache.ServerCache;
import com.avaje.ebean.cache.ServerCacheOptions;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
 * zPermissions main class.
 * 
 * @author asaddi
 */
public class ZPermissionsPlugin extends JavaPlugin {

    // Name of the default group, in absence of a config file
    private static final String DEFAULT_GROUP = "default";

    // Name of the default track, in absence of a config file
    private static final String DEFAULT_TRACK = "default";

    // Default dump directory
    private static final String DEFAULT_DUMP_DIRECTORY = "zPermissions-dumps";

    // Default timeout for temporary permissions
    private static final int DEFAULT_TEMP_PERMISSION_TIMEOUT = 60;

    // Default max-idle for Avaje bean cache
    private static final int DEFAULT_CACHE_IDLE = 180;

    // Default TTL for Avaje bean cache
    private static final int DEFAULT_CACHE_TTL = 600;

    // Default size of Avaje bean cache
    private static final int DEFAULT_CACHE_SIZE = 1000;

    // Default value for kick-on-error
    private static final boolean DEFAULT_KICK_ON_ERROR = true;

    // Default value for kick-ops-on-error
    private static final boolean DEFAULT_KICK_OPS_ON_ERROR = false;

    // Default value for region-support
    private static final boolean DEFAULT_REGION_SUPPORT_ENABLE = true;

    // Default value for assigned-groups-can-include-default
    private static final boolean DEFAULT_ASSIGNED_GROUPS_CAN_INCLUDE_DEFAULT = true;

    // Default max attempts (after the first) to complete a transaction
    private static final int DEFAULT_TXN_MAX_RETRIES = 1;

    // This plugin's logger
    private final Logger logger = Logger.getLogger(getClass().getName());

    // Version info (may include build number)
    private VersionInfo versionInfo;

    // Internal state kept about each online player
    private final Map<String, PlayerState> playerStates = new HashMap<String, PlayerState>();

    // Permission resolver
    private final PermissionsResolver resolver = new PermissionsResolver(this);

    // Our own Configuration (don't bother with JavaPlugin's)
    private FileConfiguration config;

    // The configured default track
    private String defaultTrack;

    // The configured dump directory
    private File dumpDirectory;

    // The configured default temporary permission timeout
    private int defaultTempPermissionTimeout;

    // The configured bean cache max-idle
    private int cacheMaxIdle;
    
    // The configured bean cache TTL
    private int cacheMaxTtl;

    // The configured bean cache size
    private int cacheSize;

    // Whether to kick users if there's any problem determining permissions
    private boolean kickOnError;

    // If kickOnError is true, whether or not to kick operators too
    private boolean kickOpsOnError;

    // If WorldGuard is present and this is true, enable region support
    private boolean regionSupportEnable;

    // Track definitions
    private Map<String, List<String>> tracks = new HashMap<String, List<String>>();

    // TransactionStrategy implementation
    private TransactionStrategy transactionStrategy;

    // TransactionStrategy that retries failed transactions
    // FIXME We use a separate TransactionStrategy because not all transactions
    // might be safe to be retried. Most simple transactions are safe to retry.
    // The ones that perform calculations or other operations (most notably
    // the rank commands) will have to be dealt with another way...
    private TransactionStrategy retryingTransactionStrategy;

    // DAO implementation
    private PermissionDao dao;

    // WorldGuard, if present
    private WorldGuardPlugin worldGuardPlugin;

    /**
     * Retrieve this plugin's TransactionStrategy
     * 
     * @return the TransactionStrategy
     */
    TransactionStrategy getTransactionStrategy() {
        return transactionStrategy;
    }

    /**
     * Retrieve this plugin's retrying TransactionStrategy
     * 
     * @return the retrying TransactionStrategy
     */
    TransactionStrategy getRetryingTransactionStrategy() {
        return retryingTransactionStrategy;
    }

    /**
     * Retrieve this plugin's DAO.
     * 
     * @return the DAO
     */
    PermissionDao getDao() {
        return dao;
    }

    // Retrieve the PermissionResolver instance
    PermissionsResolver getResolver() {
        return resolver;
    }

    @Override
    public void onLoad() {
        versionInfo = ToHUtils.getVersion(this);
    }

    /* (non-Javadoc)
     * @see org.bukkit.plugin.Plugin#onDisable()
     */
    @Override
    public void onDisable() {
        // Shut off monitor task, if running
        getServer().getScheduler().cancelTasks(this);

        // Clear any player state

        // Make copy before clearing
        Map<String, PlayerState> copy; 
        synchronized (playerStates) {
            copy = new HashMap<String, PlayerState>(playerStates);
            playerStates.clear();
        }
        // Remove attachments
        for (PlayerState playerState : copy.values()) {
            playerState.getAttachment().remove();
        }

        log(this, "%s disabled.", versionInfo.getVersionString());
    }

    /* (non-Javadoc)
     * @see org.bukkit.plugin.Plugin#onEnable()
     */
    @Override
    public void onEnable() {
        log(this, "%s starting...", versionInfo.getVersionString());

        // Read config
        config = ToHFileUtils.getConfig(this);
        config.options().header(null);
        readConfig();

        // Upgrade/create config
        ToHFileUtils.upgradeConfig(this, config);

        // Create database schema, if needed
        try {
            getDatabase().createQuery(Entry.class).findRowCount();
        }
        catch (PersistenceException e) {
            log(this, "Creating SQL tables...");
            installDDL();
            log(this, "Done.");
        }

        // Set up TransactionStrategy and DAO
        transactionStrategy = new AvajeTransactionStrategy(getDatabase());
        retryingTransactionStrategy = new RetryingAvajeTransactionStrategy(getDatabase(), DEFAULT_TXN_MAX_RETRIES); // TODO make configurable?
        dao = new AvajePermissionDao(getDatabase());
        applyCacheSettings();

        // Install our commands
        (new ToHCommandExecutor<ZPermissionsPlugin>(this, new RootCommands(this))).registerCommands();

        // Detect WorldGuard
        worldGuardPlugin = (WorldGuardPlugin)getServer().getPluginManager().getPlugin("WorldGuard");
        boolean regionSupport = worldGuardPlugin != null && regionSupportEnable;

        // Install our listeners
        (new ZPermissionsPlayerListener(this)).registerEvents();
        if (regionSupport) {
            (new ZPermissionsRegionPlayerListener(this)).registerEvents();
            log(this, "WorldGuard region support enabled.");
        }

        // Set up service API
        getServer().getServicesManager().register(ZPermissionsService.class, new ZPermissionsServiceImpl(getResolver(), getDao(), getRetryingTransactionStrategy()), this, ServicePriority.Normal);

        // Make sure everyone currently online has an attachment
        refreshPlayers();
        
        log(this, "%s enabled.", versionInfo.getVersionString());
    }

    // Apply cache settings to Avaje bean caches
    private void applyCacheSettings() {
        if (cacheMaxIdle <= 0 && cacheMaxTtl <= 0 && cacheSize <= 0) return; // nothing to do
        for (Class<?> clazz : getDatabaseClasses()) {
            ServerCache beanCache = getDatabase().getServerCacheManager().getBeanCache(clazz);
            ServerCacheOptions beanCacheOptions = beanCache.getOptions();
            if (cacheMaxIdle > 0)
                beanCacheOptions.setMaxIdleSecs(cacheMaxIdle);
            if (cacheMaxTtl > 0)
                beanCacheOptions.setMaxSecsToLive(cacheMaxTtl);
            if (cacheSize > 0)
                beanCacheOptions.setMaxSize(cacheSize);
            beanCache.setOptions(beanCacheOptions);
        }
    }

    /* (non-Javadoc)
     * @see org.bukkit.plugin.java.JavaPlugin#getDatabaseClasses()
     */
    @Override
    public List<Class<?>> getDatabaseClasses() {
        List<Class<?>> result = new ArrayList<Class<?>>();
        result.add(PermissionEntity.class);
        result.add(PermissionRegion.class);
        result.add(PermissionWorld.class);
        result.add(Entry.class);
        result.add(Membership.class);
        return result;
    }

    // Remove all state associated with a player, including their attachment
    void removeAttachment(String playerName) {
        PlayerState playerState = null;
        synchronized (playerStates) {
            Player player = getServer().getPlayerExact(playerName);
            if (player != null) {
                debug(this, "Removing attachment for %s", player.getName());
                playerState = playerStates.remove(player.getName());
            }
        }
        if (playerState != null)
            playerState.getAttachment().remove(); // potential to call a callback, so do it outside synchronized block
    }
    
    // Update state about a player, resolving effective permissions and
    // creating/updating their attachment
    void updateAttachment(String playerName, Location location, boolean force) {
        Player player = getServer().getPlayerExact(playerName);
        if (player == null) return;
        updateAttachment(player, location, force, false);
    }

    // Update state about a player, resolving effective permissions and
    // creating/updating their attachment
    void updateAttachment(Player player, Location location, boolean force, boolean ignoreRemoved) {
        try {
            updateAttachmentInternal(player, location, force, ignoreRemoved);
        }
        catch (Error e) {
            throw e; // Never catch errors
        }
        catch (Throwable t) {
            error(this, "Exception while updating attachment for %s", player.getName(), t);
            broadcastAdmin(this, colorize("{RED}SEVERE error while determining permissions; see server.log!"));
            
            // Kick the player, if configured to do so
            if (kickOnError && (kickOpsOnError || !player.isOp())) {
                // Probably safer to do this synchronously
                final String playerName = player.getName();
                getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                    @Override
                    public void run() {
                        Player player = getServer().getPlayerExact(playerName);
                        if (player != null)
                            player.kickPlayer("Error determining your permissions");
                    }
                });
            }
            else {
                // Ensure player has no attachment
                removeAttachment(player.getName());
                sendMessage(player, colorize("{RED}Error determining your permissions; all permissions removed!"));
            }
        }
    }

    // Simulate failures probabilistically
//    private final java.util.Random failureChance = new java.util.Random();
//    private void fakeFailureChance() {
//        if (failureChance.nextDouble() < 0.2)
//            throw new RuntimeException("Oh noes! An error!");
//    }

    // Update state about a player, resolving effective permissions and
    // creating/updating their attachment
    private void updateAttachmentInternal(Player player, Location location, boolean force, boolean ignoreRemoved) {
        final Set<String> regions = getRegions(location);

        PlayerState playerState = null;
        synchronized (playerStates) {
            playerState = playerStates.get(player.getName());
        }

        // Check if the player changed regions/worlds or isn't known yet
        if (!force) {
            force = playerState == null ||
                !regions.equals(playerState.getRegions()) ||
                !location.getWorld().getName().equals(playerState.getWorld());
        }

        // No need to update yet (most likely called by movement-based event)
        if (!force) return;

        debug(this, "Updating attachment for %s", player.getName());
        debug(this, "  location = %s", location);
        debug(this, "  regions = %s", regions);

        // Resolve effective permissions
        final String playerName2 = player.getName(); // prefer the one from Player object
        final String world = location.getWorld().getName().toLowerCase();
        Map<String, Boolean> permissions = getRetryingTransactionStrategy().execute(new TransactionCallback<Map<String, Boolean>>() {
            @Override
            public Map<String, Boolean> doInTransaction() throws Exception {
//                fakeFailureChance();
                return getResolver().resolvePlayer(playerName2, world, regions);
            }
        });

        // Create attachment and set its permissions
        PermissionAttachment pa = player.addAttachment(this);
        for (Map.Entry<String, Boolean> me : permissions.entrySet()) {
            pa.setPermission(me.getKey(), me.getValue());
        }

        // Update state
        PermissionAttachment old = null;
        synchronized (playerStates) {
            // NB: Must re-fetch since player could have been removed since first fetch
            Player newPlayer = getServer().getPlayerExact(player.getName());
            if ((newPlayer == null && !ignoreRemoved) || newPlayer != null) player = newPlayer;
            if (player != null) {
                playerState = playerStates.get(player.getName());
                if (playerState == null) {
                    // Doesn't exist yet, add it
                    playerState = new PlayerState(pa, regions, location.getWorld().getName());
                    playerStates.put(player.getName(), playerState);
                }
                else if (playerState != null) {
                    // Update values
                    old = playerState.setAttachment(pa);
                    playerState.getRegions().clear();
                    playerState.getRegions().addAll(regions);
                    playerState.setWorld(location.getWorld().getName());
                }
            }
        }
        
        // Remove old attachment, if there was one
        if (old != null)
            old.remove();
    }

    // Returns names of regions that contain the location
    private Set<String> getRegions(Location location) {
        WorldGuardPlugin wgp = getWorldGuardPlugin();
        if (wgp != null && regionSupportEnable) {
            RegionManager rm = wgp.getRegionManager(location.getWorld());
            ApplicableRegionSet ars = rm.getApplicableRegions(location);

            Set<String> result = new HashSet<String>();
            for (ProtectedRegion pr : ars) {
                // Ignore global region
                if (!"__global__".equals(pr.getId())) // NB: Hardcoded and not available as constant in WorldGuard
                    result.add(pr.getId().toLowerCase());
            }
            return result;
        }
        return Collections.emptySet();
    }

    /**
     * Refresh a particular player's attachment (and therefore, effective
     * permissions). Only does something if the player is actually online.
     * 
     * @param playerName the name of the player
     */
    void refreshPlayer(String playerName) {
        Player player = getServer().getPlayerExact(playerName);
        if (player != null) {
            debug(this, "Refreshing player %s", playerName);
            updateAttachment(playerName, player.getLocation(), true);
        }
    }

    /**
     * Refresh the attachments of all online players.
     */
    void refreshPlayers() {
        debug(this, "Refreshing all online players");
        for (Player player : getServer().getOnlinePlayers()) {
            updateAttachment(player.getName(), player.getLocation(), true);
        }
    }

    /**
     * Retrieve the configured default track.
     * 
     * @return the default track
     */
    String getDefaultTrack() {
        return defaultTrack;
    }

    /**
     * Retrieve the list of groups for the given track.
     * 
     * @param trackName the name of the track
     * @return a list of groups (in ascending order) associated with the track
     */
    List<String> getTrack(String trackName) {
        return tracks.get(trackName);
    }

    /**
     * Returns the configured dump directory.
     * 
     * @return the dump directory
     */
    File getDumpDirectory() {
        return dumpDirectory;
    }

    /**
     * Returns the configured default temporary permission timeout.
     * 
     * @return the temp permission timeout in seconds
     */
    int getDefaultTempPermissionTimeout() {
        return defaultTempPermissionTimeout;
    }

    // Read config.yml
    private void readConfig() {
        // Barebones defaults
        getResolver().setDefaultGroup(DEFAULT_GROUP);
        defaultTrack = DEFAULT_TRACK;
        dumpDirectory = new File(DEFAULT_DUMP_DIRECTORY);
        getResolver().setGroupPermissionFormats(null);
        getResolver().setAssignedGroupPermissionFormats(null);
        tracks.clear();
        
        String value;
        
        // Read values, set accordingly
        Object strOrList = config.get("group-permission");
        if (strOrList != null) {
            if (strOrList instanceof String) {
                if (hasText((String)strOrList))
                    getResolver().setGroupPermissionFormats(Collections.singleton((String)strOrList));
            }
            else if (strOrList instanceof List<?>) {
                Set<String> groupPerms = new HashSet<String>();
                for (Object obj : (List<?>)strOrList) {
                    if (obj instanceof String) {
                        groupPerms.add((String)obj);
                    }
                    else
                        warn(this, "group-permission list contains non-string value");
                }
                getResolver().setGroupPermissionFormats(groupPerms);
            }
            else
                warn(this, "group-permission must be a string or list of strings");
        }

        // TODO refactor
        strOrList = config.get("assigned-group-permission");
        if (strOrList != null) {
            if (strOrList instanceof String) {
                if (hasText((String)strOrList))
                    getResolver().setAssignedGroupPermissionFormats(Collections.singleton((String)strOrList));
            }
            else if (strOrList instanceof List<?>) {
                Set<String> groupPerms = new HashSet<String>();
                for (Object obj : (List<?>)strOrList) {
                    if (obj instanceof String) {
                        groupPerms.add((String)obj);
                    }
                    else
                        warn(this, "assigned-group-permission list contains non-string value");
                }
                getResolver().setAssignedGroupPermissionFormats(groupPerms);
            }
            else
                warn(this, "assigned-group-permission must be a string or list of strings");
        }

        getResolver().setIncludeDefaultInAssigned(config.getBoolean("assigned-groups-can-include-default", DEFAULT_ASSIGNED_GROUPS_CAN_INCLUDE_DEFAULT));

        value = config.getString("default-group");
        if (hasText(value))
            getResolver().setDefaultGroup(value);
        
        value = config.getString("default-track");
        if (hasText(value))
            defaultTrack = value;

        value = config.getString("dump-directory");
        if (hasText(value))
            dumpDirectory = new File(value);

        defaultTempPermissionTimeout = config.getInt("default-temp-permission-timeout", DEFAULT_TEMP_PERMISSION_TIMEOUT);
        cacheMaxIdle = config.getInt("cache-max-idle", DEFAULT_CACHE_IDLE);
        cacheMaxTtl = config.getInt("cache-max-ttl", DEFAULT_CACHE_TTL);
        cacheSize = config.getInt("cache-size", DEFAULT_CACHE_SIZE);

        // Read tracks, if any
        ConfigurationSection node = config.getConfigurationSection("tracks");
        if (node != null) {
            for (String trackName : node.getKeys(false)) {
                List<?> list = node.getList(trackName);
                if (list == null) {
                    warn(this, "Track %s must have a list value", trackName);
                    continue;
                }

                List<String> members = new ArrayList<String>();
                for (Object o : list) {
                    members.add(o.toString());
                }
                tracks.put(trackName, members);
            }
        }

        kickOnError = config.getBoolean("kick-on-error", DEFAULT_KICK_ON_ERROR);
        kickOpsOnError = config.getBoolean("kick-ops-on-error", DEFAULT_KICK_OPS_ON_ERROR);
        regionSupportEnable = config.getBoolean("region-support", DEFAULT_REGION_SUPPORT_ENABLE);

        // Set debug logging
        logger.setLevel(config.getBoolean("debug", false) ? Level.FINE : null);
    }

    /**
     * Re-read config.yml and refresh attachments of all online players.
     */
    synchronized void reload() {
        config = ToHFileUtils.getConfig(this);
        readConfig();
        refreshPlayers();
    }

    // Returns WorldGuardPlugin or null if not present
    private WorldGuardPlugin getWorldGuardPlugin() {
        return worldGuardPlugin;
    }

    /**
     * Give a little warning if the player isn't online.
     * 
     * @param playerName the player name
     */
    void checkPlayer(CommandSender sender, String playerName) {
        if (getServer().getPlayerExact(playerName) == null) {
            sendMessage(sender, colorize("{GRAY}(Player not online, make sure the name is correct)"));
        }
    }

    // Encapsulates state about a player
    private static class PlayerState {
        
        private PermissionAttachment attachment;

        private final Set<String> regions = new HashSet<String>();

        private String world;

        public PlayerState(PermissionAttachment attachment, Set<String> regions, String world) {
            setAttachment(attachment);
            getRegions().addAll(regions);
            setWorld(world);
        }

        public PermissionAttachment getAttachment() {
            return attachment;
        }

        public PermissionAttachment setAttachment(PermissionAttachment attachment) {
            if (attachment == null)
                throw new IllegalArgumentException("attachment cannot be null");
            PermissionAttachment old = this.attachment;
            this.attachment = attachment;
            return old;
        }

        public Set<String> getRegions() {
            return regions;
        }

        public String getWorld() {
            return world;
        }

        public void setWorld(String world) {
            if (world == null)
                throw new IllegalArgumentException("world cannot be null");
            this.world = world;
        }

    }

}
