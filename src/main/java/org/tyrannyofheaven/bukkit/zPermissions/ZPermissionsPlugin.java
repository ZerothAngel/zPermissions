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

import static org.tyrannyofheaven.bukkit.util.ToHUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.copyResourceToFile;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.hasText;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.sendMessage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.persistence.PersistenceException;

import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.ConfigurationNode;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.command.ToHCommandExecutor;
import org.tyrannyofheaven.bukkit.util.transaction.AvajeTransactionStrategy;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.dao.AvajePermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionRegion;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionWorld;

import com.sk89q.worldedit.Vector;
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

    // Internal state kept about each online player
    private final Map<String, PlayerState> playerStates = new HashMap<String, PlayerState>();

    // The configured default group
    private String defaultGroup;

    // The configured default track
    private String defaultTrack;

    // The configured group permission node format
    private String groupPermissionFormat;

    // The configured dump directory
    private File dumpDirectory;

    // The configured default temporary permission timeout
    private int defaultTempPermissionTimeout;

    // Track definitions
    private Map<String, List<String>> tracks = new HashMap<String, List<String>>();

    // TransactionStrategy implementation
    private TransactionStrategy transactionStrategy;

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
     * Retrive this plugin's DAO.
     * 
     * @return the DAO
     */
    PermissionDao getDao() {
        return dao;
    }

    /* (non-Javadoc)
     * @see org.bukkit.plugin.Plugin#onDisable()
     */
    @Override
    public void onDisable() {
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

        log("%s disabled.", getDescription().getVersion());
    }

    /* (non-Javadoc)
     * @see org.bukkit.plugin.Plugin#onEnable()
     */
    @Override
    public void onEnable() {
        log("%s starting...", getDescription().getVersion());

        // Create data directory, if needed
        if (!getDataFolder().exists())
            getDataFolder().mkdirs();

        // Create config file, if needed
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            copyResourceToFile(this, "config.yml", configFile);
            // Re-load config
            getConfiguration().load();
        }

        // Read config
        readConfig();

        // Create database schema, if needed
        try {
            getDatabase().createQuery(Entry.class).findRowCount();
        }
        catch (PersistenceException e) {
            log("Creating SQL tables...");
            installDDL();
            log("Done.");
        }

        // Set up TransactionStrategy and DAO
        transactionStrategy = new AvajeTransactionStrategy(getDatabase());
        dao = new AvajePermissionDao(getDatabase());

        // Install our commands
        CommandExecutor ce = new ToHCommandExecutor<ZPermissionsPlugin>(this, new RootCommand());
        getCommand("perm").setExecutor(ce);
        getCommand("promote").setExecutor(ce);
        getCommand("demote").setExecutor(ce);

        // Install our listeners
        ZPermissionsPlayerListener pl = new ZPermissionsPlayerListener(this);
        getServer().getPluginManager().registerEvent(Type.PLAYER_JOIN, pl, Priority.Monitor, this);
        getServer().getPluginManager().registerEvent(Type.PLAYER_KICK, pl, Priority.Monitor, this);
        getServer().getPluginManager().registerEvent(Type.PLAYER_QUIT, pl, Priority.Monitor, this);
        getServer().getPluginManager().registerEvent(Type.PLAYER_TELEPORT, pl, Priority.Monitor, this);

        // Detect WorldGuard
        worldGuardPlugin = (WorldGuardPlugin)getServer().getPluginManager().getPlugin("WorldGuard");
        if (worldGuardPlugin != null) {
            // Only check PLAYER_MOVE if region support is enabled
            getServer().getPluginManager().registerEvent(Type.PLAYER_MOVE, pl, Priority.Monitor, this);
            log("WorldGuard region support enabled.");
        }

        // Make sure everyone currently online has an attachment
        refreshPlayers();
        
        log("%s enabled.", getDescription().getVersion());
    }

    /**
     * Log a message at INFO level.
     * 
     * @param format the message format
     * @param args format args
     */
    public void log(String format, Object... args) {
        ToHUtils.log(this, Level.INFO, format, args);
    }

    /**
     * Log a message at WARNING level.
     * 
     * @param format the message format
     * @param args format args
     */
    public void warn(String format, Object... args) {
        ToHUtils.log(this, Level.WARNING, format, args);
    }

    /**
     * Log a message at FINE level.
     * 
     * @param format the message format
     * @param args format args
     */
    public void debug(String format, Object... args) {
        ToHUtils.log(this, Level.FINE, format, args);
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

    // Resolve a group's permissions. Ancestor permissions should be overridden
    // each successive descendant.
    private void resolveGroup(Map<String, Boolean> permissions, Set<String> regions, String world, PermissionEntity group) {
        // Build list of group ancestors
        List<PermissionEntity> ancestry = new ArrayList<PermissionEntity>();
        ancestry.add(group);
        while (group.getParent() != null) {
            group = group.getParent();
            ancestry.add(group);
        }
        
        // Reverse list (will be applying farthest ancestors first)
        Collections.reverse(ancestry);
        
        for (PermissionEntity ancestor : ancestry) {
            applyPermissions(permissions, ancestor, regions, world);
            
            // Add group permission, if present
            if (groupPermissionFormat != null) {
                permissions.put(String.format(groupPermissionFormat, ancestor.getName()), Boolean.TRUE);
            }
        }
    }

    // Resolve a player's permissions. Any permissions declared on the player
    // should override group permissions.
    private Map<String, Boolean> resolvePlayer(final Player player, final Set<String> regions) {
        final String world = player.getWorld().getName().toLowerCase();

        return getTransactionStrategy().execute(new TransactionCallback<Map<String, Boolean>>() {
            @Override
            public Map<String, Boolean> doInTransaction() throws Exception {
                // Get this player's groups
                List<PermissionEntity> groups = getDao().getGroups(player.getName());
                if (groups.isEmpty()) {
                    // If no groups, use the default group
                    PermissionEntity defaultGroup = getDao().getEntity(getDefaultGroup(), true);
                    if (defaultGroup != null)
                        groups.add(defaultGroup);
                }

                Map<String, Boolean> permissions = new HashMap<String, Boolean>();

                // Resolve each group in turn (highest priority resolved last)
                for (PermissionEntity group : groups) {
                    resolveGroup(permissions, regions, world, group);
                }

                // Player-specific permissions overrides all group permissions
                PermissionEntity playerEntity = getDao().getEntity(player.getName(), false);
                if (playerEntity != null) {
                    applyPermissions(permissions, playerEntity, regions, world);
                }

                return permissions;
            }
        });
    }

    // Apply an entity's permissions to the permission map. Global permissions
    // (ones not assigned to any specific world) are applied first. They are
    // then overidden by any world-specific permissions.
    private void applyPermissions(Map<String, Boolean> permissions, PermissionEntity entity, Set<String> regions, String world) {
        Map<String, Boolean> regionPermissions = new HashMap<String, Boolean>();
        List<Entry> worldPermissions = new ArrayList<Entry>();

        // Apply non-region-specific, non-world-specific permissions first
        for (Entry e : entity.getPermissions()) {
            if (e.getRegion() == null && e.getWorld() == null) {
                permissions.put(e.getPermission(), e.isValue());
            }
            else if (e.getRegion() != null && e.getWorld() == null) {
                // Global region-specific (should these really be supported?)
                if (regions.contains(e.getRegion().getName()))
                    regionPermissions.put(e.getPermission(), e.isValue());
            }
            else if (e.getWorld().getName().equals(world)) {
                worldPermissions.add(e);
            }
        }

        // Then override with world-specific permissions
        Map<String, Boolean> regionWorldPermissions = new HashMap<String, Boolean>();
        for (Entry e : worldPermissions) {
            if (e.getRegion() == null) {
                // Non region-specific
                permissions.put(e.getPermission(), e.isValue());
            }
            else {
                if (regions.contains(e.getRegion().getName()))
                    regionWorldPermissions.put(e.getPermission(), e.isValue());
            }
        }
        
        // Override with global, region-specific permissions
        permissions.putAll(regionPermissions);

        // Finally, override with region- and world-specific permissions
        permissions.putAll(regionWorldPermissions);
    }

    // Remove all state associated with a player, including their attachment
    void removeAttachment(String playerName) {
        PlayerState playerState = null;
        synchronized (playerStates) {
            Player player = getPlayerExact(playerName);
            if (player != null)
                playerState = playerStates.remove(player.getName());
        }
        if (playerState != null)
            playerState.getAttachment().remove(); // potential to call a callback, so do it outside synchronized block
    }

    // Update state about a player, resolving effective permissions and
    // creating/updating their attachment
    void updateAttachment(String playerName, Set<String> regions, boolean force) {
        Player player;
        PlayerState playerState = null;
        synchronized (playerStates) {
            player = getPlayerExact(playerName);
            if (player != null)
                playerState = playerStates.get(player.getName());
        }

        if (player == null) return;

        // Check if the player changed regions/worlds or isn't known yet
        if (!force) {
            force = playerState == null ||
                !regions.equals(playerState.getRegions()) ||
                !player.getWorld().getName().equals(playerState.getWorld());
        }

        // No need to update yet (most likely called by movement-based event)
        if (!force) return;

        // Resolve effective permissions
        PermissionAttachment pa = player.addAttachment(this);
        for (Map.Entry<String, Boolean> me : resolvePlayer(player, regions).entrySet()) {
            pa.setPermission(me.getKey(), me.getValue());
        }

        // Update state
        PermissionAttachment old = null;
        synchronized (playerStates) {
            // NB: Must re-fetch since player could have been removed since first fetch
            player = getPlayerExact(playerName);
            if (player != null) {
                playerState = playerStates.get(player.getName());
                if (playerState == null) {
                    // Doesn't exist yet, add it
                    playerState = new PlayerState(pa, regions, player.getWorld().getName());
                    playerStates.put(player.getName(), playerState);
                }
                else if (playerState != null) {
                    // Update values
                    old = playerState.setAttachment(pa);
                    playerState.getRegions().clear();
                    playerState.getRegions().addAll(regions);
                    playerState.setWorld(player.getWorld().getName());
                }
            }
        }
        
        // Remove old attachment, if there was one
        if (old != null)
            old.remove();
    }

    // FIXME Added to Server in CB1087
    private Player getPlayerExact(String playerName) {
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(playerName))
                return player;
        }
        return null;
    }

    // Returns names of regions the player is currently inside
    Set<String> getPlayerRegions(String playerName) {
        Player player = getPlayerExact(playerName);
        if (player != null)
            return getRegions(player.getLocation());
        return Collections.emptySet();
    }

    // Returns names of regions the player is currently inside
    Set<String> getRegions(Location location) {
        WorldGuardPlugin wgp = getWorldGuardPlugin();
        if (wgp != null) {
            RegionManager rm = wgp.getRegionManager(location.getWorld());
            //ApplicableRegionSet ars = rm.getApplicableRegions(player.getLocation()); FIXME dev worldedit
            ApplicableRegionSet ars = rm.getApplicableRegions(new Vector(location.getX(), location.getY(), location.getZ()));

            Set<String> result = new HashSet<String>();
            for (ProtectedRegion pr : ars) {
                // Ignore global region
                if (!"__global__".equals(pr.getId())) // FIXME hardcoded
                    result.add(pr.getId());
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
        updateAttachment(playerName, getPlayerRegions(playerName), true);
    }

    /**
     * Refresh the attachments of all online players.
     */
    void refreshPlayers() {
        for (Player player : getServer().getOnlinePlayers()) {
            updateAttachment(player.getName(), getRegions(player.getLocation()), true);
        }
    }

    /**
     * Retrieve the configured default group.
     * 
     * @return the default group
     */
    String getDefaultGroup() {
        return defaultGroup;
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
    public int getDefaultTempPermissionTimeout() {
        return defaultTempPermissionTimeout;
    }

    // Read config.yml
    private void readConfig() {
        // Barebones defaults
        defaultGroup = DEFAULT_GROUP;
        defaultTrack = DEFAULT_TRACK;
        dumpDirectory = new File(DEFAULT_DUMP_DIRECTORY);
        groupPermissionFormat = null;
        tracks.clear();
        
        String value;
        
        // Read values, set accordingly
        value = (String)getConfiguration().getProperty("group-permission");
        if (hasText(value))
            groupPermissionFormat = value;

        value = (String)getConfiguration().getProperty("default-group");
        if (hasText(value))
            defaultGroup = value;
        
        value = (String)getConfiguration().getProperty("default-track");
        if (hasText(value))
            defaultTrack = value;

        value = (String)getConfiguration().getProperty("dump-directory");
        if (hasText(value))
            dumpDirectory = new File(value);

        defaultTempPermissionTimeout = getConfiguration().getInt("default-temp-permission-timeout", DEFAULT_TEMP_PERMISSION_TIMEOUT);

        // Read tracks, if any
        ConfigurationNode node = getConfiguration().getNode("tracks");
        if (node != null) {
            for (String trackName : node.getKeys()) {
                List<Object> list = node.getList(trackName);
                if (list == null) {
                    warn("Track %s must have a list value", trackName);
                    continue;
                }

                List<String> members = new ArrayList<String>();
                for (Object o : list) {
                    if (!(o instanceof String)) {
                        warn("Track %s contains non-string value", trackName);
                        continue;
                    }
                    members.add((String)o);
                }
                tracks.put(trackName, members);
            }
        }
    }

    /**
     * Re-read config.yml and refresh attachments of all online players.
     */
    synchronized void reload() {
        getConfiguration().load();
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
        if (getPlayerExact(playerName) == null) {
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
