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

import static org.tyrannyofheaven.bukkit.util.ToHUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.copyResourceToFile;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.hasText;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.sendMessage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.persistence.PersistenceException;

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
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionWorld;

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

    // Maps player names to attachments. All access must be synchronized to
    // attachments.
    private final Map<String, PermissionAttachment> attachments = new HashMap<String, PermissionAttachment>();

    // Maps player names to world names. All access must be synchronized to
    // attachments.
    private final Map<String, String> lastWorld = new HashMap<String, String>();

    // The configured default group
    private String defaultGroup;

    // The configured default track
    private String defaultTrack;

    // The configured group permission node format
    private String groupPermissionFormat;

    // Track definitions
    private Map<String, List<String>> tracks = new HashMap<String, List<String>>();

    // TransactionStrategy implementation
    private TransactionStrategy transactionStrategy;

    // DAO implementation
    private PermissionDao dao;

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
        synchronized (lastWorld) {
            lastWorld.clear();
        }

        // Remove our attachments
        Map<String, PermissionAttachment> copy;
        synchronized (attachments) {
            copy = new HashMap<String, PermissionAttachment>(attachments);
            attachments.clear();
        }
        for (PermissionAttachment pa : copy.values()) {
            pa.remove();
        }

        log("%s disabled.", getDescription().getVersion());
    }

    /* (non-Javadoc)
     * @see org.bukkit.plugin.Plugin#onEnable()
     */
    @Override
    public void onEnable() {
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
        result.add(PermissionWorld.class);
        result.add(Entry.class);
        result.add(Membership.class);
        return result;
    }

    // Resolve a group's permissions. Ancestor permissions should be overridden
    // each successive descendant.
    private void resolveGroup(Map<String, Boolean> permissions, String world, PermissionEntity group) {
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
            applyPermissions(permissions, ancestor, world);
            
            // Add group permission, if present
            if (groupPermissionFormat != null) {
                permissions.put(String.format(groupPermissionFormat, ancestor.getName()), Boolean.TRUE);
            }
        }
    }

    // Resolve a player's permissions. Any permissions declared on the player
    // should override group permissions.
    private Map<String, Boolean> resolvePlayer(final Player player) {
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
                    resolveGroup(permissions, world, group);
                }

                // Player-specific permissions overrides all group permissions
                PermissionEntity playerEntity = getDao().getEntity(player.getName(), false);
                if (playerEntity != null) {
                    applyPermissions(permissions, playerEntity, world);
                }

                return permissions;
            }
        });
    }

    // Apply an entity's permissions to the permission map. Global permissions
    // (ones not assigned to any specific world) are applied first. They are
    // then overidden by any world-specific permissions.
    private void applyPermissions(Map<String, Boolean> permissions, PermissionEntity entity, String world) {
        Map<String, Boolean> worldPermissions = new HashMap<String, Boolean>();

        // Apply non-world-specific permissions first
        for (Entry e : entity.getPermissions()) {
            if (e.getWorld() == null) {
                permissions.put(e.getPermission(), e.isValue());
            }
            else if (e.getWorld().getName().equals(world)) {
                worldPermissions.put(e.getPermission(), e.isValue());
            }
        }

        // Then override with world-specific permissions
        permissions.putAll(worldPermissions);
    }

    void removeAttachment(Player player) {
        PermissionAttachment pa;
        synchronized (attachments) {
            pa = attachments.get(player.getName());
            if (pa != null)
                attachments.remove(player.getName());
        }
        if (pa != null)
            pa.remove(); // potential to call a callback, so do it outside synchronized block
    }

    void updateAttachment(Player player, boolean force) {
        // Check if the player changed worlds
        if (!force) {
            String playerLastWorld;
            synchronized (lastWorld) {
                playerLastWorld = lastWorld.get(player.getName());
            }
            
            force = !player.getWorld().getName().equals(playerLastWorld);
        }

        if (!force) return;

        synchronized (lastWorld) {
            lastWorld.put(player.getName(), player.getWorld().getName());
        }

        PermissionAttachment pa = player.addAttachment(this);
        for (Map.Entry<String, Boolean> me : resolvePlayer(player).entrySet()) {
            pa.setPermission(me.getKey(), me.getValue());
        }

        PermissionAttachment old;
        synchronized (attachments) {
            old = attachments.put(player.getName(), pa);
        }
        if (old != null)
            old.remove();
    }

    /**
     * Refresh a particular player's attachment (and therefore, effective
     * permissions). Only does something if the player is actually online.
     * 
     * @param playerName the name of the player
     */
    void refreshPlayer(String playerName) {
        Player player = getServer().getPlayer(playerName);
        if (player != null)
            updateAttachment(player, true);
    }

    /**
     * Refresh the attachments of all online players.
     */
    void refreshPlayers() {
        for (Player player : getServer().getOnlinePlayers()) {
            updateAttachment(player, true);
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

    // Read config.yml
    private void readConfig() {
        // Barebones defaults
        defaultGroup = DEFAULT_GROUP;
        defaultTrack = DEFAULT_TRACK;
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

    /**
     * Give a little warning if the player isn't online.
     * 
     * @param playerName the player name
     */
    void checkPlayer(CommandSender sender, String playerName) {
        if (getServer().getPlayer(playerName) == null) {
            sendMessage(sender, colorize("{GRAY}(Player not online, make sure the name is correct)"));
        }
    }

}
