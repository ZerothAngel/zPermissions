package org.tyrannyofheaven.bukkit.zPermissions;

import static org.tyrannyofheaven.bukkit.util.ToHUtils.copyResourceToFile;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.hasText;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.persistence.PersistenceException;

import org.bukkit.command.CommandExecutor;
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

public class ZPermissionsPlugin extends JavaPlugin {

    private static final String DEFAULT_GROUP = "default";
    
    private static final String DEFAULT_TRACK = "default";

    private final Map<String, PermissionAttachment> attachments = new HashMap<String, PermissionAttachment>();

    private final Map<String, String> lastWorld = new HashMap<String, String>();

    private String defaultGroup;
    
    private String defaultTrack;
    
    private String groupPermissionFormat;

    private Map<String, List<String>> tracks = new HashMap<String, List<String>>();

    private TransactionStrategy transactionStrategy;

    private PermissionDao dao;

    TransactionStrategy getTransactionStrategy() {
        return transactionStrategy;
    }

    PermissionDao getDao() {
        return dao;
    }

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

        readConfig();

        try {
            getDatabase().createQuery(Entry.class).findRowCount();
        }
        catch (PersistenceException e) {
            log("Creating SQL tables...");
            installDDL();
            log("Done.");
        }
        
        transactionStrategy = new AvajeTransactionStrategy(getDatabase());
        dao = new AvajePermissionDao(getDatabase());
        
        CommandExecutor ce = new ToHCommandExecutor<ZPermissionsPlugin>(this, new RootCommand());
        getCommand("perm").setExecutor(ce);
        getCommand("promote").setExecutor(ce);
        getCommand("demote").setExecutor(ce);

        ZPermissionsPlayerListener pl = new ZPermissionsPlayerListener(this);
        getServer().getPluginManager().registerEvent(Type.PLAYER_JOIN, pl, Priority.Monitor, this);
        getServer().getPluginManager().registerEvent(Type.PLAYER_KICK, pl, Priority.Monitor, this);
        getServer().getPluginManager().registerEvent(Type.PLAYER_QUIT, pl, Priority.Monitor, this);
        getServer().getPluginManager().registerEvent(Type.PLAYER_TELEPORT, pl, Priority.Monitor, this);

        refreshPlayers();
        
        log("%s enabled.", getDescription().getVersion());
    }

    public void log(String format, Object... args) {
        ToHUtils.log(this, Level.INFO, format, args);
    }

    public void warn(String format, Object... args) {
        ToHUtils.log(this, Level.WARNING, format, args);
    }

    public void debug(String format, Object... args) {
        ToHUtils.log(this, Level.FINE, format, args);
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {
        List<Class<?>> result = new ArrayList<Class<?>>();
        result.add(PermissionEntity.class);
        result.add(PermissionWorld.class);
        result.add(Entry.class);
        result.add(Membership.class);
        return result;
    }

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

    private Map<String, Boolean> resolvePlayer(final Player player) {
        final String world = player.getWorld().getName().toLowerCase();

        return getTransactionStrategy().execute(new TransactionCallback<Map<String, Boolean>>() {
            @Override
            public Map<String, Boolean> doInTransaction() throws Exception {
                // TODO Auto-generated method stub
                List<PermissionEntity> groups = getDao().getGroups(player.getName());
                if (groups.isEmpty()) {
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

    void refreshPlayer(String playerName) {
        Player player = getServer().getPlayer(playerName);
        if (player != null)
            updateAttachment(player, true);
    }

    void refreshPlayers() {
        for (Player player : getServer().getOnlinePlayers()) {
            updateAttachment(player, true);
        }
    }

    String getDefaultGroup() {
        return defaultGroup;
    }

    String getDefaultTrack() {
        return defaultTrack;
    }

    List<String> getTrack(String trackName) {
        return tracks.get(trackName);
    }

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

    synchronized void reload() {
        getConfiguration().load();
        readConfig();
        refreshPlayers();
    }

}
