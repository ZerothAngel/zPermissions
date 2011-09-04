package org.tyrannyofheaven.bukkit.zPermissions;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class ZPermissionsPlayerListener extends PlayerListener {

    private final ZPermissionsPlugin plugin;
    
    ZPermissionsPlayerListener(ZPermissionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.addAttachment(event.getPlayer());
    }

    @Override
    public void onPlayerKick(PlayerKickEvent event) {
        if (event.isCancelled()) return;
        
        plugin.removeAttachment(event.getPlayer());
    }

    @Override
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled()) return;
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.removeAttachment(event.getPlayer());
    }

}
