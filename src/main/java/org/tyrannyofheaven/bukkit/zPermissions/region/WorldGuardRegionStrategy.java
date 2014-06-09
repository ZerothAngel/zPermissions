/*
 * Copyright 2013 Allan Saddi <allan@saddi.com>
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
package org.tyrannyofheaven.bukkit.zPermissions.region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.util.ToHLoggingUtils;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsCore;

import com.google.common.collect.Iterables;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
 * RegionStrategy implementation for WorldGuard.
 * 
 * @author asaddi
 */
public class WorldGuardRegionStrategy implements RegionStrategy, Listener {

    private static final String RM_PLUGIN_NAME = "WorldGuard";

    private final Plugin plugin;

    private final ZPermissionsCore core;

    private WorldGuardPlugin worldGuardPlugin;

    public WorldGuardRegionStrategy(Plugin plugin, ZPermissionsCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    @Override
    public String getName() {
        return RM_PLUGIN_NAME;
    }

    @Override
    public boolean isPresent() {
        return Bukkit.getPluginManager().getPlugin(RM_PLUGIN_NAME) != null;
    }

    @Override
    public void init() {
        detectWorldGuardPlugin();
        if (!isEnabled()) {
            // Not yet loaded, listen for its enable event
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }
    }

    @Override
    public boolean isEnabled() {
        return worldGuardPlugin != null;
    }

    @Override
    public void shutdown() {
        worldGuardPlugin = null;
    }

    @Override
    public Set<String> getRegions(Location location, Player player) {
        if (isEnabled()) {
            RegionManager rm = worldGuardPlugin.getRegionManager(location.getWorld());
            if (rm != null) {
                ApplicableRegionSet ars = rm.getApplicableRegions(location);
                // Note, sorted from high to low priority, i.e. reverse application order
                List<ProtectedRegion> sorted = new ArrayList<>();
                Iterables.addAll(sorted, ars);
                Collections.reverse(sorted); // Now it is in application order

                Set<String> result = new LinkedHashSet<>(); // Preserve ordering for resolver
                for (ProtectedRegion pr : sorted) {
                    // Ignore global region
                    if (!"__global__".equals(pr.getId())) // NB: Hardcoded and not available as constant in WorldGuard
                        result.add(pr.getId().toLowerCase());
                }
                return result;
            }
        }
        return Collections.emptySet();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (!isEnabled() && RM_PLUGIN_NAME.equals(event.getPlugin().getName())) {
            detectWorldGuardPlugin();
            if (isEnabled()) {
                ToHLoggingUtils.log(plugin, "%s region support enabled.", getName());
                core.refreshPlayers();
            }
        }
    }

    private void detectWorldGuardPlugin() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(RM_PLUGIN_NAME);
        if (plugin instanceof WorldGuardPlugin && plugin.isEnabled()) {
            worldGuardPlugin = (WorldGuardPlugin)plugin;
        }
        else {
            worldGuardPlugin = null;
        }
    }

}
