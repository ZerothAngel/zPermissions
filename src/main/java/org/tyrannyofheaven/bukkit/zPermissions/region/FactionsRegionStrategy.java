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

import java.util.Collections;
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

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.BoardColls;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.UPlayer;
import com.massivecraft.mcore.ps.PS;

/**
 * RegionStrategy implementation for Factions.
 * 
 * @author asaddi
 */
public class FactionsRegionStrategy implements RegionStrategy, Listener {

    private static final String RM_PLUGIN_NAME = "Factions";

    private final Plugin plugin;

    private final ZPermissionsCore core;

    private boolean factionsEnabled;

    public FactionsRegionStrategy(Plugin plugin, ZPermissionsCore core) {
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
        detectFactionsPlugin();
        if (!isEnabled()) {
            // Not yet loaded, listen for its enable event
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }
    }

    @Override
    public boolean isEnabled() {
        return factionsEnabled;
    }

    @Override
    public void shutdown() {
        factionsEnabled = false;
    }

    @Override
    public Set<String> getRegions(Location location, Player player) {
        if (isEnabled()) {
            // This indirection is necessary to avoid NoClassDefErrors when
            // Factions is not present.
            return FactionsHelper.getRegions(location, player);
        }
        return Collections.emptySet();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (!isEnabled() && RM_PLUGIN_NAME.equals(event.getPlugin().getName())) {
            detectFactionsPlugin();
            if (isEnabled()) {
                ToHLoggingUtils.log(plugin, "%s region support enabled.", getName());
                core.refreshPlayers();
            }
        }
    }

    private void detectFactionsPlugin() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(RM_PLUGIN_NAME);
        factionsEnabled = plugin.isEnabled();
    }

    private static class FactionsHelper {

        private static Set<String> getRegions(Location location, Player player) {
            Faction faction = BoardColls.get().getFactionAt(PS.valueOf(location));
            UPlayer uplayer = UPlayer.get(player);
            if (faction != null && uplayer != null) {
                Rel rel = uplayer.getRelationTo(faction);
                if (rel != null) {
                    return Collections.singleton(rel.name().toLowerCase());
                }
            }
            return Collections.emptySet();
        }

    }

}
