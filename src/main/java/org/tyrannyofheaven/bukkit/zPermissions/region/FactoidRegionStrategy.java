/*
 * Copyright 2013 ZerothAngel <zerothangel@tyrannyofheaven.org>
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

import java.util.Collections;
import java.util.Set;

import me.tabinol.factoid.Factoid;
import me.tabinol.factoid.lands.Land;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.util.ToHLoggingUtils;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsCore;

/**
 * RegionStrategy implementation for Factoid.
 *
 * @author zerothangel, Tabinol
 */
public class FactoidRegionStrategy implements RegionStrategy, Listener {

    private static final String RM_PLUGIN_NAME = "Factoid";

    private final Plugin plugin;

    private final ZPermissionsCore core;

    private boolean enabled;

    public FactoidRegionStrategy(Plugin plugin, ZPermissionsCore core) {
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
        detectFactoidPlugin();
        if (!isEnabled()) {
            // Not yet loaded, listen for its enable event
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void shutdown() {
        enabled = false;
    }

    @Override
    public Set<String> getRegions(Location location, Player player) {
        Land land = Factoid.getLands().getLand(location);
        if (land != null) {
            return Collections.singleton(land.getName()); // Land name is always lower case
        }
        return Collections.emptySet();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (!isEnabled() && RM_PLUGIN_NAME.equals(event.getPlugin().getName())) {
            detectFactoidPlugin();
            if (isEnabled()) {
                ToHLoggingUtils.log(plugin, "%s region support enabled.", getName());
                core.refreshPlayers();
            }
        }
    }

    private void detectFactoidPlugin() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(RM_PLUGIN_NAME);
        enabled = plugin != null && plugin.isEnabled();
        // Nothing else to do
    }

}
