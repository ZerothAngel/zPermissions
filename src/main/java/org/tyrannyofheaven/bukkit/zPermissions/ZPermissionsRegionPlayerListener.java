/*
 * Copyright 2011, 2012 Allan Saddi <allan@saddi.com>
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

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Additional player events to listen on if (WorldGuard) region support is
 * enabled.
 * 
 * @author asaddi
 */
public class ZPermissionsRegionPlayerListener implements Listener {

    private final ZPermissionsPlugin plugin;

    ZPermissionsRegionPlayerListener(ZPermissionsPlugin plugin) {
        this.plugin = plugin;
    }

    void registerEvents() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled()) return;

        // Conditionally update if world changed
        plugin.updateAttachment(event.getPlayer(), event.getTo(), false, false);
    }

    @EventHandler(priority=EventPriority.MONITOR)
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

}
