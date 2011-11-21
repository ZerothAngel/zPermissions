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

import static org.tyrannyofheaven.bukkit.util.ToHUtils.registerEvent;

import org.bukkit.event.Event.Priority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * PlayerListener for zPermissions. Simply updates or removes the zPermissions
 * attachment as appropriate.
 * 
 * @author asaddi
 */
class ZPermissionsPlayerListener extends PlayerListener {

    private final ZPermissionsPlugin plugin;
    
    ZPermissionsPlayerListener(ZPermissionsPlugin plugin) {
        this.plugin = plugin;
    }

    void registerEvents(boolean regionSupport) {
        registerEvent("PLAYER_LOGIN", this, Priority.Lowest, plugin);
        registerEvent("PLAYER_QUIT", this, Priority.Monitor, plugin);
        if (!registerEvent("PLAYER_CHANGED_WORLD", this, Priority.Lowest, plugin)) {
            registerEvent("PLAYER_TELEPORT", this, Priority.Monitor, plugin);
        }

        // Only check PLAYER_MOVE if region support is enabled
        if (regionSupport)
            registerEvent("PLAYER_MOVE", this, Priority.Monitor, plugin);
    }

    @Override
    public void onPlayerLogin(PlayerLoginEvent event) {
        plugin.updateAttachment(event.getPlayer(), event.getPlayer().getLocation(), true, true);
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.removeAttachment(event.getPlayer().getName());
    }

    @Override
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled()) return;

        // Conditionally update if world changed
        plugin.updateAttachment(event.getPlayer(), event.getTo(), false, false);
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.isCancelled()) return;

        // Only bother if player actually moved to a new block
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockY() != event.getTo().getBlockY() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            // Conditionally update if containing regions changed
            plugin.updateAttachment(event.getPlayer(), event.getTo(), false, false);
        }
    }

    @Override
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        plugin.updateAttachment(event.getPlayer(), event.getPlayer().getLocation(), false, false);
    }

}
