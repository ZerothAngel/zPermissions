/*
 * Copyright 2011, 2012 ZerothAngel <zerothangel@tyrannyofheaven.org>
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
package org.tyrannyofheaven.bukkit.zPermissions.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.tyrannyofheaven.bukkit.zPermissions.RefreshCause;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsCore;

/**
 * Additional player events to listen on if (WorldGuard) region support is
 * enabled.
 * 
 * @author zerothangel
 */
public class ZPermissionsRegionPlayerListener implements Listener {

    private final ZPermissionsCore core;

    public ZPermissionsRegionPlayerListener(ZPermissionsCore plugin) {
        this.core = plugin;
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Conditionally update if world or region changed
        core.setBukkitPermissions(event.getPlayer(), event.getTo(), false, RefreshCause.MOVEMENT);
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only bother if player actually moved to a new block
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockY() != event.getTo().getBlockY() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            // Conditionally update if containing regions changed
            core.setBukkitPermissions(event.getPlayer(), event.getTo(), false, RefreshCause.MOVEMENT);
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Conditionally update if respawning in a different world or region
        core.setBukkitPermissions(event.getPlayer(), event.getRespawnLocation(), false, RefreshCause.MOVEMENT);
    }

}
