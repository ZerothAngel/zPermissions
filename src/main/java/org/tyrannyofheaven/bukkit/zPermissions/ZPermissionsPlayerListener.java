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

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * PlayerListener for zPermissions. Simply updates or removes the zPermissions
 * attachment as appropriate.
 * 
 * @author zerothangel
 */
class ZPermissionsPlayerListener extends PlayerListener {

    private final ZPermissionsPlugin plugin;
    
    ZPermissionsPlayerListener(ZPermissionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.updateAttachment(event.getPlayer().getName(), plugin.getPlayerRegions(event.getPlayer()), true);
    }

    @Override
    public void onPlayerKick(PlayerKickEvent event) {
        if (event.isCancelled()) return;
        
        plugin.removeAttachment(event.getPlayer().getName());
    }

    @Override
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled()) return;

        // Conditionally update if world changed
        plugin.updateAttachment(event.getPlayer().getName(), plugin.getPlayerRegions(event.getPlayer()), false);
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.removeAttachment(event.getPlayer().getName());
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.isCancelled()) return;

        // Only bother if player actually moved to a new block
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockY() != event.getTo().getBlockY() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            // Conditionally update if containing regions changed
            plugin.updateAttachment(event.getPlayer().getName(), plugin.getPlayerRegions(event.getPlayer()), false);
        }
    }

}
