/*
 * Copyright 2011 ZerothAngel <zerothangel@tyrannyofheaven.org>
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
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionRemovedExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.tyrannyofheaven.bukkit.util.ToHDatabaseUtils;
import org.tyrannyofheaven.bukkit.util.ToHFileUtils;
import org.tyrannyofheaven.bukkit.util.ToHNamingConvention;
import org.tyrannyofheaven.bukkit.util.ToHSchemaVersion;
import org.tyrannyofheaven.bukkit.util.ToHStringUtils;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.VersionInfo;
import org.tyrannyofheaven.bukkit.util.command.ToHCommandExecutor;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.PermissionsResolver.ResolverResult;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionRegion;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionWorld;
import org.tyrannyofheaven.bukkit.zPermissions.service.ZPermissionsServiceImpl;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
 * zPermissions main class.
 * 
 * @author zerothangel
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

    // Default database support
    private static final boolean DEFAULT_DATABASE_SUPPORT = true;

    // Default number of ticks to wait between attachment refreshes of all players
    private static final int DEFAULT_BULK_REFRESH_DELAY = 5;

    // Default opaque inheritance
    private static final boolean DEFAULT_OPAQUE_INHERITANCE = true;

    // Default auto-refresh interval
    private static final int DEFAULT_AUTO_REFRESH_INTERVAL = -1;

    // Filename of file-based storage
    private static final String FILE_STORAGE_FILENAME = "data.yml";

    // Name of metadata key for our PlayerState instances
    private static final String PLAYER_METADATA_KEY = "zPermissions.PlayerState";

    // Version info (may include build number)
    private VersionInfo versionInfo;

    // Permission resolver
    private final PermissionsResolver resolver = new PermissionsResolver(this);

    // Model dumper
    private final ModelDumper modelDumper = new ModelDumper(this);

    // Multi-user refreshing
    private final RefreshTask refreshTask = new RefreshTask(this);

    // Our own Configuration (don't bother with JavaPlugin's)
    private FileConfiguration config;

    // The configured default track
    private String defaultTrack;

    // The configured dump directory
    private File dumpDirectory;

    // The configured default temporary permission timeout
    private int defaultTempPermissionTimeout;

    // Whether to kick users if there's any problem determining permissions
    private boolean kickOnError;

    // If kickOnError is true, whether or not to kick operators too
    private boolean kickOpsOnError;

    // If WorldGuard is present and this is true, enable region support
    private boolean regionSupportEnable;

    // Track definitions
    private Map<String, List<String>> tracks = new LinkedHashMap<String, List<String>>();

    // Whether or not to use the database (Avaje) storage strategy
    private boolean databaseSupport;

    // Maximum number of times to retry transactions (so total attempts is +1)
    private int txnMaxRetries;

    // Interval for auto-refresh
    private int autoRefreshInterval;

    // Task ID for auto-refresh task
    private int autoRefreshTaskId = -1;

    // Strategy for permissions storage
    private StorageStrategy storageStrategy;

    // WorldGuard, if present
    private WorldGuardPlugin worldGuardPlugin;

    // Create our own instance rather than use Bukkit's
    private EbeanServer ebeanServer;

    // Custom NamingConvention for Avaje
    private final ToHNamingConvention namingConvention = new ToHNamingConvention(this, "zperms_schema_version");

    /**
     * Retrieve this plugin's TransactionStrategy
     * 
     * @return the TransactionStrategy
     */
    TransactionStrategy getTransactionStrategy() {
        return storageStrategy.getTransactionStrategy();
    }

    /**
     * Retrieve this plugin's retrying TransactionStrategy
     * FIXME We use a separate TransactionStrategy because not all transactions
     * might be safe to be retried. Most simple transactions are safe to retry.
     * The ones that perform calculations or other operations (most notably
     * the rank commands) will have to be dealt with another way...
     * 
     * @return the retrying TransactionStrategy
     */
    TransactionStrategy getRetryingTransactionStrategy() {
        return storageStrategy.getRetryingTransactionStrategy();
    }

    /**
     * Retrieve this plugin's DAO.
     * 
     * @return the DAO
     */
    PermissionDao getDao() {
        return storageStrategy.getDao();
    }

    // Retrieve the PermissionResolver instance
    PermissionsResolver getResolver() {
        return resolver;
    }

    // Retrieve ModelDumper instance
    ModelDumper getModelDumper() {
        return modelDumper;
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
        // Kill pending refresh, if any
        refreshTask.stop();

        // Really shut off all async tasks
        getServer().getScheduler().cancelTasks(this);

        // Ensure storage is shut down properly
        storageStrategy.shutdown();

        // Clear any player state

        // Remove attachments
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerState playerState = getPlayerState(player);
            if (playerState != null) {
                playerState.removeAttachment();
            }
            player.removeMetadata(PLAYER_METADATA_KEY, this);
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

        // Set up TransactionStrategy and DAO
        storageStrategy = null;
        if (databaseSupport) {
            ebeanServer = ToHDatabaseUtils.createEbeanServer(this, getClassLoader(), namingConvention);

            SpiEbeanServer spiEbeanServer = (SpiEbeanServer)ebeanServer;
            if (spiEbeanServer.getDatabasePlatform().getName().contains("sqlite")) {
                log(this, Level.WARNING, "This plugin is NOT compatible with SQLite.");
                log(this, Level.WARNING, "Edit bukkit.yml to switch databases or disable database support in config.yml.");
                log(this, Level.WARNING, "Falling back to file-based storage strategy.");
                // Do nothing else (storageStrategy still null)
            }
            else {
                try {
                    ToHDatabaseUtils.upgradeDatabase(this, namingConvention, getClassLoader(), "sql");
                }
                catch (IOException e) {
                    error(this, "Exception upgrading database schema:", e);
                }

                log(this, "Using database storage strategy.");
                storageStrategy = new AvajeStorageStrategy(this, txnMaxRetries); // TODO make configurable?
            }
        }
        
        // If still no storage strategy at this point, use flat-file one
        if (storageStrategy == null) {
            log(this, "Using file-based storage strategy.");
            storageStrategy = new MemoryStorageStrategy(this, new File(getDataFolder(), FILE_STORAGE_FILENAME));
        }
        
        // Initialize storage strategy
        try {
            storageStrategy.init();
        }
        catch (Exception e) {
            error(this, "Exception initializing storage strategy:", e);
            // TODO Now what?
        }

        // Install our commands
        (new ToHCommandExecutor<ZPermissionsPlugin>(this, new RootCommands(this, resolver)))
            .registerTypeCompleter("group", new GroupTypeCompleter(getDao()))
            .registerTypeCompleter("track", new TrackTypeCompleter(this))
            .registerTypeCompleter("dump-dir", new DirTypeCompleter(this))
            .registerCommands();

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

        // Start auto-refresh task, if one is configured
        startAutoRefreshTask();

        log(this, "%s enabled.", versionInfo.getVersionString());
    }

    @Override
    public EbeanServer getDatabase() {
        return ebeanServer;
    }

    /* (non-Javadoc)
     * @see org.bukkit.plugin.java.JavaPlugin#getDatabaseClasses()
     */
    @Override
    public List<Class<?>> getDatabaseClasses() {
        List<Class<?>> result = new ArrayList<Class<?>>();
        result.add(ToHSchemaVersion.class);
        result.add(PermissionEntity.class);
        result.add(PermissionRegion.class);
        result.add(PermissionWorld.class);
        result.add(Entry.class);
        result.add(Membership.class);
        return result;
    }

    // Remove all state associated with a player, including their attachment
    void removeAttachment(Player player) {
        debug(this, "Removing attachment for %s", player.getName());
        PlayerState playerState = getPlayerState(player);
        if (playerState != null) {
            playerState.removeAttachment(); // potential to call a callback
        }
        player.removeMetadata(PLAYER_METADATA_KEY, this);
    }
    
    // Update state about a player, resolving effective permissions and
    // creating/updating their attachment
    void updateAttachment(Player player, Location location, boolean force) {
        try {
            updateAttachmentInternal(player, location, force);
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
                removeAttachment(player);
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
    private void updateAttachmentInternal(final Player player, Location location, boolean force) {
        final Set<String> regions = getRegions(location);

        PlayerState playerState = getPlayerState(player);

        // Check if the player changed regions/worlds or isn't known yet
        // (Or if attachment mysteriously disappeared...)
        if (!force) {
            force = playerState == null ||
                !regions.equals(playerState.getRegions()) ||
                !location.getWorld().getName().equals(playerState.getWorld()) ||
                playerState.getAttachment() == null;
        }

        // No need to update yet (most likely called by movement-based event)
        if (!force) return;

        debug(this, "Updating attachment for %s", player.getName());
        debug(this, "  location = %s", location);
        debug(this, "  regions = %s", regions);

        // Resolve effective permissions
        final String world = location.getWorld().getName().toLowerCase();
        ResolverResult resolverResult = getRetryingTransactionStrategy().execute(new TransactionCallback<ResolverResult>() {
            @Override
            public ResolverResult doInTransaction() throws Exception {
//                fakeFailureChance();
                return getResolver().resolvePlayer(player.getName(), world, regions);
            }
        });

        // Create attachment and set its permissions
        PermissionAttachment pa = null;
        boolean created = false;

        if (playerState != null) {
            // Re-use old attachment
            pa = playerState.getAttachment(); // NB may be null due to premature removal
        }
        
        if (pa == null) {
            // Create brand new one, if needed
            pa = player.addAttachment(this);
            created = true;
        }

        debug(this, "(Existing PlayerState = %s, existing attachment = %s)", playerState != null, !created);

        boolean succeeded = false;
        try {
            Field perms = pa.getClass().getDeclaredField("permissions");
            perms.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Boolean> privatePerms = (Map<String, Boolean>)perms.get(pa);
            privatePerms.clear();
            privatePerms.putAll(resolverResult.getPermissions());
            pa.getPermissible().recalculatePermissions();
            succeeded = true;
        }
        catch (SecurityException e) {
            // Do nothing
        }
        catch (NoSuchFieldException e) {
            // Do nothing
        }
        catch (IllegalArgumentException e) {
            // Do nothing
        }
        catch (IllegalAccessException e) {
            // Do nothing
        }
        if (!succeeded) {
            // The slow, but legal way
            warn(this, "Setting permissions the slow way. Is zPermissions up-to-date?");
            if (!created)
                pa = player.addAttachment(this); // create new one to start from clean slate
            // Set each permission individually... which unfortunately calls recalculatePermissions each time
            for (Map.Entry<String, Boolean> me : resolverResult.getPermissions().entrySet()) {
                pa.setPermission(me.getKey(), me.getValue());
            }
        }

        // Update state
        PermissionAttachment old = null;
        if (playerState == null) {
            // Doesn't exist yet, add it
            playerState = new PlayerState(pa, regions, location.getWorld().getName(), resolverResult.getGroups());
            player.setMetadata(PLAYER_METADATA_KEY, new FixedMetadataValue(this, playerState));
        }
        else {
            // Update values
            old = playerState.setAttachment(pa);
            playerState.setRegions(regions);
            playerState.setWorld(location.getWorld().getName());
            playerState.setGroups(resolverResult.getGroups());
        }
        
        // Remove old attachment, if there was one and it's different (avoids recalculatePermissions call)
        if (old != null && old != pa)
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
        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null) {
            debug(this, "Refreshing player %s", player.getName());
            updateAttachment(player, player.getLocation(), true);
        }
    }

    /**
     * Refresh the attachments of all online players.
     */
    void refreshPlayers() {
        debug(this, "Refreshing all online players");
        Set<String> toRefresh = new HashSet<String>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            toRefresh.add(player.getName());
        }
        refreshTask.start(toRefresh);
    }

    /**
     * Refresh all players who are members of the given group.
     * 
     * @param groupName the affected group
     */
    void refreshAffectedPlayers(String groupName) {
        groupName = groupName.toLowerCase();
        Set<String> toRefresh = new HashSet<String>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerState playerState = getPlayerState(player);
            if (playerState == null || playerState.getGroups().contains(groupName)) {
                toRefresh.add(player.getName());
            }
        }
        
        if (toRefresh.isEmpty())
            return; // Nothing to do

        if (getLogger().isLoggable(Level.FINE))
            debug(this, "Refreshing players: %s", ToHStringUtils.delimitedString(", ", toRefresh));
        refreshTask.start(toRefresh);
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
     * Retrieve names of all tracks.
     * 
     * @return names of all tracks
     */
    List<String> getTracks() {
        return new ArrayList<String>(tracks.keySet());
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
        databaseSupport = config.getBoolean("database-support", DEFAULT_DATABASE_SUPPORT);
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

        getResolver().setOpaqueInheritance(config.getBoolean("opaque-inheritance", DEFAULT_OPAQUE_INHERITANCE));
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
        txnMaxRetries = config.getInt("txn-max-retries", DEFAULT_TXN_MAX_RETRIES); // FIXME hidden

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
        // Set up default track if none are defined
        if (tracks.isEmpty()) {
            List<String> members = new ArrayList<String>();
            members.add("default");
            members.add("somegroup");
            members.add("someothergroup");
            tracks.put("default", members);
        }

        kickOnError = config.getBoolean("kick-on-error", DEFAULT_KICK_ON_ERROR);
        kickOpsOnError = config.getBoolean("kick-ops-on-error", DEFAULT_KICK_OPS_ON_ERROR);
        regionSupportEnable = config.getBoolean("region-support", DEFAULT_REGION_SUPPORT_ENABLE);

        // FIXME currently hidden option
        refreshTask.setDelay(config.getInt("bulk-refresh-delay", DEFAULT_BULK_REFRESH_DELAY));
        autoRefreshInterval = config.getInt("auto-refresh-interval", DEFAULT_AUTO_REFRESH_INTERVAL);

        ToHDatabaseUtils.populateNamingConvention(config, namingConvention);

        // Set debug logging
        getLogger().setLevel(config.getBoolean("debug", false) ? Level.FINE : null);
    }

    /**
     * Re-read config.yml and refresh attachments of all online players.
     */
    void reload() {
        config = ToHFileUtils.getConfig(this);
        readConfig();
        startAutoRefreshTask();
        refresh(new Runnable() {
            @Override
            public void run() {
                refreshPlayers();
            }
        });
    }

    /**
     * Refresh permissions store
     */
    void refresh(Runnable finishTask) {
        storageStrategy.refresh(finishTask);
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

    // Cancel existing auto-refresh task and start a new one if autoRefreshInterval is valid
    private void startAutoRefreshTask() {
        // Cancel previous task, if any
        if (autoRefreshTaskId > -1) {
            Bukkit.getScheduler().cancelTask(autoRefreshTaskId);
            autoRefreshTaskId = -1;
        }
        // Start up new task at new interval
        if (autoRefreshInterval > 0) {
            final Plugin plugin = this;
            autoRefreshTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
                @Override
                public void run() {
                    log(plugin, "Refreshing from database...");
                    refresh(new Runnable() {
                        @Override
                        public void run() {
                            // This is executed after the storage refresh is done.
                            log(plugin, "Refresh done.");
                            refreshPlayers();
                        }
                    });
                }
            }, autoRefreshInterval * 20 * 60, autoRefreshInterval * 20 * 60); // FIXME magic numbers
        }
    }

    // Retrieve associated PlayerState, if any
    private PlayerState getPlayerState(Player player) {
        for (MetadataValue mv : player.getMetadata(PLAYER_METADATA_KEY)) {
            if (mv.getOwningPlugin() == this) {
                return (PlayerState)mv.value();
            }
        }
        return null;
    }

    // Encapsulates state about a player
    private static class PlayerState implements PermissionRemovedExecutor {
        
        private PermissionAttachment attachment;

        private Set<String> regions;

        private String world;

        private Set<String> groups;

        public PlayerState(PermissionAttachment attachment, Set<String> regions, String world, Set<String> groups) {
            setAttachment(attachment);
            setRegions(regions);
            setWorld(world);
            setGroups(groups);
        }

        public PermissionAttachment getAttachment() {
            return attachment;
        }

        public PermissionAttachment setAttachment(PermissionAttachment attachment) {
            if (attachment == null)
                throw new IllegalArgumentException("attachment cannot be null");
            PermissionAttachment old = this.attachment;
            this.attachment = attachment;
            this.attachment.setRemovalCallback(this);
            return old;
        }

        public void removeAttachment() {
            if (attachment != null)
                attachment.remove();
            attachment = null;
        }

        public void setRegions(Set<String> regions) {
            // NB should already be lower-cased
            this.regions = Collections.unmodifiableSet(new HashSet<String>(regions));
        }

        public Set<String> getRegions() {
            return regions;
        }

        public String getWorld() {
            return world;
        }

        public Set<String> getGroups() {
            return groups;
        }

        public void setWorld(String world) {
            if (world == null)
                throw new IllegalArgumentException("world cannot be null");
            this.world = world;
        }

        public void setGroups(Set<String> groups) {
            this.groups = new HashSet<String>(groups.size());
            for (String group : groups) {
                this.groups.add(group.toLowerCase());
            }
            this.groups = Collections.unmodifiableSet(this.groups);
        }

        @Override
        public void attachmentRemoved(PermissionAttachment attachment) {
            if (attachment == this.attachment)
                this.attachment = null;
        }

    }

}
