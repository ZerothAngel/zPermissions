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
package org.tyrannyofheaven.bukkit.zPermissions.event;

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.debug;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.zPermissions.RefreshCause;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsCore;

/**
 * PlayerListener for zPermissions. Simply updates or removes the zPermissions
 * attachment as appropriate.
 * 
 * @author zerothangel
 */
public class ZPermissionsPlayerListener implements Listener {

    private final ZPermissionsCore core;
    
    private final Plugin plugin;

    public ZPermissionsPlayerListener(ZPermissionsCore core, Plugin plugin) {
        this.core = core;
        this.plugin = plugin;
    }

    // Do this early for the benefit of anything listening on the same event
    @EventHandler(priority=EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        debug(plugin, "%s logged in", event.getPlayer().getName());
        core.updateAttachment(event.getPlayer(), event.getPlayer().getLocation(), true, null);
        // NB don't bother with expirations until join
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerLoginMonitor(PlayerLoginEvent event) {
        // If they aren't sticking around...
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            // Forget about them
            debug(plugin, "%s is not allowed to log in", event.getPlayer().getName());
            core.removeAttachment(event.getPlayer());
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        debug(plugin, "%s joining", event.getPlayer().getName());
        // NB eventCause is null because it's a given that the player's permissions has changed on join
        // (ignore the fact that it actually changed on login for now)
        core.updateAttachment(event.getPlayer(), event.getPlayer().getLocation(), true, null); // Does this need to be forced again?
        // Wait for next tick...
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                core.refreshExpirations();
            }
        });
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        debug(plugin, "%s quitting", event.getPlayer().getName());
        core.removeAttachment(event.getPlayer());
        // Wait for next tick...
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                core.refreshExpirations();
            }
        });
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        core.updateAttachment(event.getPlayer(), event.getPlayer().getLocation(), false, RefreshCause.MOVEMENT);
    }

}
